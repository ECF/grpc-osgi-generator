# grpc-osgi-generator
Service interface generator plugin for grpc.  This project implements a plugin for [Google's Protocol Buffers](https://developers.google.com/protocol-buffers) protoc code generator.  It's designed to work alongside the [grpc-java compiler](https://github.com/grpc/grpc-java).

What grpc-java does

The grpc-java protoc plugin takes a service declaration from within a .proto file and generates java source code dependent upon grpc.  For example [here](https://raw.githubusercontent.com/ECF/grpc-RemoteServicesProvider/master/examples/org.eclipse.ecf.examples.provider.grpc.health.api/src/main/proto/health.proto) is a grpc example proto file:

<pre>
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
  // If the requested service is unknown, the call will fail with status
  // NOT_FOUND.
  rpc check(HealthCheckRequest) returns (HealthCheckResponse);

  // Performs a watch for the serving status of the requested service.
  // The server will immediately send back a message indicating the current
  // serving status.  It will then subsequently send a new message whenever
  // the service's serving status changes.
  //
  // If the requested service is unknown when the call is received, the
  // server will send a message setting the serving status to
  // SERVICE_UNKNOWN but will *not* terminate the call.  If at some
  // future point, the serving status of the service becomes known, the
  // server will send a new message with the service's serving status.
  //
  // If the call terminates with status UNIMPLEMENTED, then clients
  // should assume this method is not supported and should not retry the
  // call.  If the call terminates with any other status (including OK),
  // clients should retry the call with appropriate exponential backoff.
  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}
</pre>








