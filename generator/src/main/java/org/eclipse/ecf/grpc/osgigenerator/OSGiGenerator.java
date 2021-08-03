/*******************************************************************************
 * Copyright (c) 2020 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.grpc.osgigenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.compiler.PluginProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;

public class OSGiGenerator extends Generator {

	private static final String JAVA_EXTENSION = ".java";
	private static final String SERVICE_CLASS_SUFFIX = "Service";
	private static final int SERVICE_NUMBER_OF_PATHS = 2;
	private static final int METHOD_NUMBER_OF_PATHS = 4;

	private static final String REACTIVE_PACKAGE = String.join(".", "io", "reactivex");
	private static final String REACTIVE3_PACKAGE = String.join(".", "io", "reactivex", "rxjava3", "core");
	private static final String FLOWABLE_CLASS = "Flowable";
	private static final String SINGLE_CLASS = "Single";

	private static final String DEFAULT_BODY_NULL_RETURN = "return null";

	public static void main(String[] args) throws Exception {
		List<Generator> generators = new ArrayList<Generator>();
		generators.add(new OSGiGenerator());
		@SuppressWarnings("rawtypes")
		List<GeneratedExtension> extensions = new ArrayList<GeneratedExtension>();
		extensions.add(OsgiServiceOptionsProto.generationType);
		extensions.add(OsgiServiceOptionsProto.interfaceMethodBodyType);
		extensions.add(OsgiServiceOptionsProto.fileReactivexVersion);
		extensions.add(OsgiServiceOptionsProto.serviceReactivexVersion);
		if (args.length == 0) {
			ProtocPlugin.generate(generators, extensions);
		} else {
			ProtocPlugin.debug(generators, extensions, args[0]);
		}
	}

	@Override
	public List<File> generateFiles(CodeGeneratorRequest request) throws GeneratorException {
		return generateFiles(findServices(request.getProtoFileList().stream()
				.filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
				.collect(Collectors.toList()), ProtoTypeMap.of(request.getProtoFileList())));
	}

	private List<ServiceContext> findServices(List<FileDescriptorProto> protos, ProtoTypeMap typeMap) {
		List<ServiceContext> contexts = new ArrayList<>();

		protos.forEach(fileProto -> {
			for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
				ServiceContext serviceContext = buildServiceContext(fileProto.getService(serviceNumber), typeMap,
						fileProto.getSourceCodeInfo().getLocationList(), serviceNumber,
						fileProto.getOptions().getExtension(OsgiServiceOptionsProto.fileReactivexVersion));
				serviceContext.protoName = fileProto.getName();
				serviceContext.packageName = extractPackageName(fileProto);
				contexts.add(serviceContext);
			}
		});

		return contexts;
	}

	private String extractPackageName(FileDescriptorProto proto) {
		FileOptions options = proto.getOptions();
		if (options != null) {
			String javaPackage = options.getJavaPackage();
			if (!Strings.isNullOrEmpty(javaPackage)) {
				return javaPackage;
			}
		}

		return Strings.nullToEmpty(proto.getPackage());
	}

	private ServiceContext buildServiceContext(ServiceDescriptorProto serviceProto, ProtoTypeMap typeMap,
			List<Location> locations, int serviceNumber, ReactiveXVersion fileReactiveXVersion) {
		String serviceName = serviceProto.getName();
		ServiceContext serviceContext = new ServiceContext();
		serviceContext.methodTypes = serviceProto.getOptions().getExtension(OsgiServiceOptionsProto.generationType);
		serviceContext.className = serviceName + SERVICE_CLASS_SUFFIX;
		serviceContext.fileName = serviceContext.className + JAVA_EXTENSION;
		serviceContext.serviceName = serviceName;
		serviceContext.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

		List<Location> allLocationsForService = locations.stream()
				.filter(location -> location.getPathCount() >= 2
						&& location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER
						&& location.getPath(1) == serviceNumber)
				.collect(Collectors.toList());

		Location serviceLocation = allLocationsForService.stream()
				.filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS).findFirst()
				.orElseGet(Location::getDefaultInstance);
		serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix());

		String servicePackage = REACTIVE_PACKAGE; // default to version 2 for now
		if (fileReactiveXVersion == ReactiveXVersion.V_3 || serviceProto.getOptions()
				.getExtension(OsgiServiceOptionsProto.serviceReactivexVersion) == ReactiveXVersion.V_3) {
			servicePackage = REACTIVE3_PACKAGE;
		}

		for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
			MethodContext methodContext = buildMethodContext(serviceContext, serviceProto.getMethod(methodNumber),
					typeMap, allLocationsForService, methodNumber, servicePackage);
			if (methodContext != null) {
				serviceContext.methods.add(methodContext);
			}
		}
		return serviceContext;
	}

	private void setImport(ServiceContext serviceContext, String importClass) {
		if (!serviceContext.imports.stream().filter(i -> i.importClass.equals(importClass)).findAny().isPresent())
			serviceContext.imports.add(new Import(importClass));
	}

	private MethodContext buildMethodContext(ServiceContext serviceContext, MethodDescriptorProto methodProto,
			ProtoTypeMap typeMap, List<Location> locations, int methodNumber, String service_package) {

		MethodContext methodContext = new MethodContext();
		methodContext.isManyInput = methodProto.getClientStreaming();
		methodContext.isManyOutput = methodProto.getServerStreaming();
		methodContext.methodName = lowerCaseFirst(methodProto.getName());
		// Get base input and output types
		String baseInputType = typeMap.toJavaTypeName(methodProto.getInputType());
		String baseOutputType = typeMap.toJavaTypeName(methodProto.getOutputType());
		// Get defaultBody if present in options and set defaultBody in methodContext
		if (methodProto.getOptions()
				.getExtension(OsgiServiceOptionsProto.interfaceMethodBodyType) == InterfaceMethodBodyType.THROW) {
			methodContext.defaultBody = String.format(
					"throw new UnsupportedOperationException(\"%s not implemented yet\")", methodContext.methodName);
		} else {
			methodContext.defaultBody = DEFAULT_BODY_NULL_RETURN;
		}
		String fq_flowable_class = String.join(".", service_package, FLOWABLE_CLASS);
		String fq_single_class = String.join(".", service_package, SINGLE_CLASS);
		// Argument types
		if (methodContext.isManyInput) {
			setImport(serviceContext, fq_flowable_class);
			methodContext.inputType = FLOWABLE_CLASS;
		} else {
			setImport(serviceContext, fq_single_class);
			methodContext.inputType = SINGLE_CLASS;
		}
		methodContext.inputGenericType = baseInputType;
		// return types
		if (methodContext.isManyOutput) {
			setImport(serviceContext, fq_flowable_class);
			methodContext.outputType = FLOWABLE_CLASS;
		} else {
			setImport(serviceContext, fq_single_class);
			methodContext.outputType = SINGLE_CLASS;
		}
		methodContext.outputGenericType = baseOutputType;

		methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
		Location methodLocation = locations.stream()
				.filter(location -> location.getPathCount() == METHOD_NUMBER_OF_PATHS
						&& location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber)
				.findFirst().orElseGet(Location::getDefaultInstance);
		methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());

		return methodContext;
	}

	private String lowerCaseFirst(String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<ServiceContext> services) {
		return services.stream().map(this::buildFile).collect(Collectors.toList());
	}

	private String getServiceJavaDocPrefix() {
		return "";
	}

	private String getMethodJavaDocPrefix() {
		return "    ";
	}

	private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context) {
		return PluginProtos.CodeGeneratorResponse.File.newBuilder().setName(absoluteFileName(context))
				.setContent(applyTemplate(context.methodTypes == GenerationType.GRPC_UNARY ? "GrpcService.mustache"
						: "ReactiveXService.mustache", context))
				.build();
	}

	private String absoluteFileName(ServiceContext ctx) {
		String dir = ctx.packageName.replace('.', '/');
		if (Strings.isNullOrEmpty(dir)) {
			return ctx.fileName;
		} else {
			return dir + "/" + ctx.fileName;
		}
	}

	private String getComments(Location location) {
		return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
	}

	private String getJavaDoc(String comments, String prefix) {
		if (!comments.isEmpty()) {
			StringBuilder builder = new StringBuilder("/**\n").append(prefix).append(" * <pre>\n");
			Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
					.map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
					.forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
			builder.append(prefix).append(" * </pre>\n").append(prefix).append(" */");
			return builder.toString();
		}
		return null;
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	private class Import {
		public String importClass;

		Import(String clazzName) {
			this.importClass = clazzName;
		}
	}

	/**
	 * Template class for proto Service objects.
	 */
	private class ServiceContext {
		public GenerationType methodTypes;
		public List<Import> imports = new ArrayList<Import>();
		// CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
		public String fileName;
		@SuppressWarnings("unused")
		public String protoName;
		public String packageName;
		public String className;
		@SuppressWarnings("unused")
		public String serviceName;
		@SuppressWarnings("unused")
		public boolean deprecated;
		@SuppressWarnings("unused")
		public String javaDoc;
		public List<MethodContext> methods = new ArrayList<>();

		@SuppressWarnings("unused")
		public List<MethodContext> unaryRequestMethods() {
			return methods.stream().filter(m -> (!m.isManyInput && !m.isManyOutput)).collect(Collectors.toList());
		}

		@SuppressWarnings("unused")
		public List<MethodContext> serverStreamingRequestMethods() {
			return methods.stream().filter(m -> (!m.isManyInput && m.isManyOutput)).collect(Collectors.toList());
		}

		@SuppressWarnings("unused")
		public List<MethodContext> clientStreamingRequestMethods() {
			return methods.stream().filter(m -> (m.isManyInput && !m.isManyOutput)).collect(Collectors.toList());
		}

		@SuppressWarnings("unused")
		public List<MethodContext> bidiStreamingRequestMethods() {
			return methods.stream().filter(m -> (m.isManyInput && m.isManyOutput)).collect(Collectors.toList());
		}

		@SuppressWarnings("unused")
		public List<Import> importClassNames() {
			return imports.stream().filter(distinctByKey(i -> i.importClass)).collect(Collectors.toList());
		}
	}

	/**
	 * Template class for proto RPC objects.
	 */
	@SuppressWarnings("unused")
	private class MethodContext {
		// CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
		public String methodName;
		public String inputType;
		public String inputGenericType;
		public String outputType;
		public String outputGenericType;
		public boolean deprecated;
		public boolean isManyInput;
		public boolean isManyOutput;
		public String javaDoc;
		public String defaultBody;

		// This method mimics the upper-casing method ogf gRPC to ensure compatibility
		// See
		// https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
		public String methodNameUpperUnderscore() {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < methodName.length(); i++) {
				char c = methodName.charAt(i);
				s.append(Character.toUpperCase(c));
				if ((i < methodName.length() - 1) && Character.isLowerCase(c)
						&& Character.isUpperCase(methodName.charAt(i + 1))) {
					s.append('_');
				}
			}
			return s.toString();
		}

		public String methodNamePascalCase() {
			String mn = methodName.replace("_", "");
			return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
		}

		public String methodNameCamelCase() {
			String mn = methodName.replace("_", "");
			return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
		}

	}

}
