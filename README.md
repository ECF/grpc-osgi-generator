# grpc-osgi-generator
This project implements a protoc plugin to generate a Java service interface class and supporting classes.

For example, consider the HealthCheck service declaration in the following proto file snippet:

```proto

service HealthCheck {
  // Unary method
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
  // Server streaming method
  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
  // Client streaming method
  rpc Watch1(stream HealthCheckRequest) returns (HealthCheckResponse);
  // bidi streaming method
  rpc Watch2(stream HealthCheckRequest) returns (stream HealthCheckResponse);
}
```
From this proto file, this plugin will generate this service interface

```java
package io.grpc.health.v1;

@javax.annotation.Generated(
value = "by OSGi Remote Services generator",
comments = "Source: health.proto")
public interface HealthCheckService {

    
    default io.grpc.health.v1.HealthCheckResponse check(io.grpc.health.v1.HealthCheckRequest request) {
        return null;
    }
    
    /**
     * <pre>
     *  Server streaming method
     * </pre>
     */
    default io.reactivex.Flowable<io.grpc.health.v1.HealthCheckResponse> watch(io.reactivex.Single<io.grpc.health.v1.HealthCheckRequest> request)  {
        return null;
    }
    
    /**
     * <pre>
     *  Client streaming method
     * </pre>
     */
    default io.reactivex.Single<io.grpc.health.v1.HealthCheckResponse> watch1(io.reactivex.Flowable<io.grpc.health.v1.HealthCheckRequest> requests)  {
        return null;
    }
    
    /**
     * <pre>
     *  bidi streaming method
     * </pre>
     */
    default io.reactivex.Flowable<io.grpc.health.v1.HealthCheckResponse> watch2(io.reactivex.Flowable<io.grpc.health.v1.HealthCheckRequest> requests)  {
        return null;
    }
}
```
As can be seen above, the generated interface class has a method corresponding to each type of [grpc](https://grpc.io/) method type:  unary (call-return), server-streaming, client-streaming, and bi-directional streaming.

This service interface can then be used for exposing [OSGi Remote Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html) in [OSGi](https://www.osgi.org) environments, since all OSGi Services are based upon the service interface class.  [ECF has an implementation](https://wiki.eclipse.org/OSGi_Remote_Services_and_ECF) of a [OSGi Remote Services Distribution Provider](https://github.com/ECF/grpc-RemoteServicesProvider) that exports and imports (along with discovery) of such grpc-based services using the OSGi Remote Service Admin specification.

The end effect of this generation is that starting with a proto service declaration such as HealthCheck above that

# This protoc plugin run alongside the grpc plugin and the reactive-grpc plugin will produce

1 Classes representing the protobuf messages and options -- via protocol buffers compiler
1 Classes providing access to grpc-based unary service methods -- via grpc-java plugin that is part of [grpc](https://github.com/grpc/)
1 Classes providing access to reactive-grpc APIs for streaming service methods (server, client, bidirectional) - via [reactive-grpc](https://github.com/salesforce/reactive-grpc)
1 A service interface class (e.g. HealthCheckService class above) that references the message types and the reactive Flowable and Single classes (see streaming HealthCheckService methods above).


## What running protoc + grpc-osgi-generator plugin + [grpc-java](https://github.com/grpc/grpc-java) plugin does

[Here](https://raw.githubusercontent.com/ECF/grpc-RemoteServicesProvider/master/examples/org.eclipse.ecf.examples.provider.grpc.health.api/src/main/proto/health.proto) is a grpc example proto file input file:

```proto
// Copyright 2015 The gRPC Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// The canonical version of this proto can be found at
// https://github.com/grpc/grpc-proto/blob/master/grpc/health/v1/health.proto

syntax = "proto3";

package grpc.health.v1;

option java_multiple_files = true;
option java_outer_classname = "HealthProto";
option java_package = "io.grpc.health.v1";

message HealthCheckRequest {
  string message = 1;
}

message HealthCheckResponse {
  enum ServingStatus {
    UNKNOWN = 0;
    SERVING = 1;
    NOT_SERVING = 2;
    SERVICE_UNKNOWN = 3;  // Used only by the Watch method.
  }
  ServingStatus status = 1;
}

service HealthCheck {
  // Unary method
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
  // Streaming method
  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}
```

Note that HealthCheckRequest and HealthCheckResponse are protocol buffers messages, and that the HealthCheck service is declared with both a unary method (Check), and a streaming method (Watch).

With the above proto file as input to protoc with the grpc-osgi-generator and grpc-java plugins the following classes are generated:

AbstractHealthCheckServiceImpl - **grpc-osgi-generator**
HealthCheckGrpc - grpc-java compiler plugin
HealthCheckRequest - protoc java
HealthCheckRequestOrBuilder - protoc java
HealthCheckResponse - protoc java
HealthCheckResponseOrBuilder - protoc java
HealthCheckService - **grpc-osgi-generator**
HealthCheckProto - grpc-java

[Here](https://github.com/ECF/grpc-RemoteServicesProvider/tree/master/examples/org.eclipse.ecf.examples.provider.grpc.health.api/src/main/java/io/grpc/health/v1) is the directory in the example with the generated source code.

The two classes generated by this plugin are a service interface class [HealthCheckService](https://github.com/ECF/grpc-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.provider.grpc.health.api/src/main/java/io/grpc/health/v1/HealthCheckService.java)

and an abstract impl of this service interface class [AbstractHealthCheckServiceImpl](https://github.com/ECF/grpc-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.provider.grpc.health.api/src/main/java/io/grpc/health/v1/AbstractHealthCheckServiceImpl.java). 

All of the other classes were generated by either protoc java directly, or by the grcp-java compiler plugin.

For OSGi Services a service interface is required (aka objectClass) for service registration, and that's why this grpc-osgi-generator plugin generates **HealthCheckService** class.   **AbstractHealthCheckServiceImpl** is an abstract helper class that implements **HealthCheckService** that service implementers can extend.  See the [grcp-RemoteServicesProvider](https://github.com/ECF/grpc-RemoteServicesProvider) project.

## Building and Installing this Plugin into local Maven repo

To build and install this plugin into your local Maven repository, clone this to your system and then

>cd <grpc-osgi-generator repo-location>
>maven install
  

## Using this protoc plugin via protobuf-maven-plugin

Currently the easiest way to use protoc with both the grpc-java plugin and this plugin is to use the protobuf-maven-plugin.

It's typically easiset to declare versions from within the Maven pom.xml properties section

```xml
<properties>
<!-- your other properties -->
	<grpc.version>1.29.0</grpc.version>
	<protoc.version>3.11.4</protoc.version>
	<jprotoc.version>1.0.1</jprotoc.version>
</properties>
```

Then in the same pom.xml these dependencies should be present

```xml
	<dependencies>
		<dependency>
			<groupId>io.grpc</groupId>
			<artifactId>grpc-netty</artifactId>
			<version>${grpc.version}</version>
		</dependency>
		<dependency>
			<groupId>io.grpc</groupId>
			<artifactId>grpc-protobuf</artifactId>
			<version>${grpc.version}</version>
		</dependency>
		<dependency>
			<groupId>com.salesforce.servicelibs</groupId>
			<artifactId>jprotoc</artifactId>
			<version>${jprotoc.version}</version>
		</dependency>
		<dependency>
			<groupId>io.grpc</groupId>
			<artifactId>grpc-core</artifactId>
			<version>${grpc.version}</version>
		</dependency>
		<dependency>
			<groupId>io.grpc</groupId>
			<artifactId>grpc-context</artifactId>
			<version>${grpc.version}</version>
		</dependency>
		<dependency>
			<groupId>io.grpc</groupId>
			<artifactId>grpc-stub</artifactId>
			<version>${grpc.version}</version>
		</dependency>
	</dependencies>
```

Finally, the following extension and plugins should be in your build section (along with any other maven plugins like compile, clean, etc)

```xml
	<build>
		<extensions>
			<extension>
				<groupId>kr.motd.maven</groupId>
				<artifactId>os-maven-plugin</artifactId>
				<version>1.6.2</version>
			</extension>
		</extensions>
		<plugins>
			<plugin>
				<groupId>org.xolstice.maven.plugins</groupId>
				<artifactId>protobuf-maven-plugin</artifactId>
				<version>0.6.1</version>
				<configuration>
					<protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
					</protocArtifact>
					<pluginId>grpc-java</pluginId>
					<pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
					</pluginArtifact>
				</configuration>
				<executions>
					<execution>
						<goals>
							<goal>compile</goal>
							<goal>compile-custom</goal>
						</goals>
						<phase>generate-sources</phase>
						<configuration>
							<protocPlugins>
								<protocPlugin>
									<id>grpc-osgi-generator</id>
									<groupId>org.eclipse.ecf</groupId>
									<artifactId>grpc-osgi-generator</artifactId>
									<version>1.0.0-SNAPSHOT</version>
									<mainClass>org.eclipse.ecf.grpc.osgigenerator.OSGiGenerator
									</mainClass>
								</protocPlugin>
							</protocPlugins>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>  
  </build>
```
Note in the above that the protoc + grpc-java plugin + grpc-osgi-generator will be run as part of a build (generate-sources phase) on **any .proto files in ./src/main/proto** directory and output to the **./target/generated-sources/java/** directory.  

