package org.eclipse.ecf.grpc.osgigenerator;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.ProtocPlugin;

public class RxJava3OSGiGenerator extends OSGiGenerator {

	public RxJava3OSGiGenerator(boolean rx3) {
		super(rx3);
	}

	public static void main(String[] args) {
		List<Generator> generators = new ArrayList<Generator>();
		generators.add(new RxJava3OSGiGenerator(true));
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

}
