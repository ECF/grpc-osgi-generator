package org.eclipse.ecf.grpc.osgigenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
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

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			ProtocPlugin.generate(new OSGiGenerator());
		} else {
			ProtocPlugin.debug(new OSGiGenerator(), args[0]);
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
						fileProto.getSourceCodeInfo().getLocationList(), serviceNumber);
				serviceContext.protoName = fileProto.getName();
				serviceContext.packageName = extractPackageName(fileProto);
				contexts.add(serviceContext);
				// also add AbstractServiceImplContext
				contexts.add(new AbstractServiceImplContext(serviceContext));
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
			List<Location> locations, int serviceNumber) {
		String serviceName = serviceProto.getName();
		ServiceContext serviceContext = new ServiceContext();
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

		for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
			MethodContext methodContext = buildMethodContext(serviceProto.getMethod(methodNumber), typeMap, locations,
					methodNumber);
			if (methodContext != null) {
				serviceContext.methods.add(methodContext);
			}
		}
		return serviceContext;
	}

	private MethodContext buildMethodContext(MethodDescriptorProto methodProto, ProtoTypeMap typeMap,
			List<Location> locations, int methodNumber) {

		if (methodProto.getClientStreaming() || methodProto.getServerStreaming()) {
			return null;
		}

		MethodContext methodContext = new MethodContext();
		methodContext.methodName = lowerCaseFirst(methodProto.getName());
		methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
		methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
		methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
		methodContext.isManyInput = methodProto.getClientStreaming();
		methodContext.isManyOutput = methodProto.getServerStreaming();
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
		String content = applyTemplate(
				(context instanceof AbstractServiceImplContext) ? "AbstractServiceImpl.mustache" : "Service.mustache", context);
		return PluginProtos.CodeGeneratorResponse.File.newBuilder().setName(absoluteFileName(context))
				.setContent(content).build();
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

	/**
	 * Template class for proto Service objects.
	 */
	private class ServiceContext {
		// CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
		public String fileName;
		public String protoName;
		public String packageName;
		public String className;
		public String serviceName;
		public boolean deprecated;
		public String javaDoc;
		public List<MethodContext> methods = new ArrayList<>();

		@SuppressWarnings("unused")
		public List<MethodContext> unaryRequestMethods() {
			return methods.stream().filter(m -> !m.isManyInput).collect(Collectors.toList());
		}
	}

	private class AbstractServiceImplContext extends ServiceContext {

		public String originalClassName;
		
		public AbstractServiceImplContext(ServiceContext svc) {
			this.protoName = svc.protoName;
			this.packageName = svc.packageName;
			this.serviceName = svc.serviceName;
			this.deprecated = svc.deprecated;
			this.javaDoc = svc.javaDoc;
			this.methods = new ArrayList<MethodContext>(svc.methods);
			this.className = "Abstract" + svc.className + "Impl";
			this.fileName = this.className + JAVA_EXTENSION;
			this.originalClassName = svc.className;
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
		public String outputType;
		public boolean deprecated;
		public boolean isManyInput;
		public boolean isManyOutput;
		public String javaDoc;

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
