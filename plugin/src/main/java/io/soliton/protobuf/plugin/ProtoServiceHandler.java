/**
 * Copyright 2013 Julien Silland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.soliton.protobuf.plugin;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class ProtoServiceHandler {

	private final String javaPackage;
	private final TypeMap types;
  private final boolean multipleFiles;
  private final String outerClassName;
  private final OutputStream output;
	
	public ProtoServiceHandler(String javaPackage, TypeMap types, boolean multipleFiles,
      String outerClassName, OutputStream output) {
		this.javaPackage = javaPackage;
		this.types = Preconditions.checkNotNull(types);
    this.multipleFiles = multipleFiles;
    this.outerClassName = multipleFiles ? null : Preconditions.checkNotNull(outerClassName);
    this.output = Preconditions.checkNotNull(output);
	}
	
	public void handle(ServiceDescriptorProto service) throws IOException {
		StringBuilder serviceFile = new StringBuilder();
    if (multipleFiles) {
      serviceFile.append("// Generated code. DO NOT EDIT!\n\n");
      if (javaPackage != null) {
        serviceFile.append("package ").append(javaPackage).append(";\n\n");
      }
    }

    String classModifiers = !multipleFiles ? "public abstract static" : "public abstract";
		serviceFile.append(String.format("%s class %s implements io.soliton.protobuf.Service {\n",
        classModifiers, service.getName()));
		
		serviceFile.append("  public String shortName() {\n");
		serviceFile.append("    return \"").append(service.getName()).append("\";\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public String fullName() {\n");
		serviceFile.append("    return \"").append(service.getName()).append("\";\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static interface Interface {\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			String javaMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName());
			serviceFile.append("    public com.google.common.util.concurrent.ListenableFuture<").append(types.lookup(method.getOutputType())).append("> ").append(javaMethodName).append("(").append(types.lookup(method.getInputType())).append(" request);\n");
		}
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static Interface newStub(final io.soliton.protobuf.Client transport) {\n");
		serviceFile.append("    final com.google.common.collect.ImmutableMap<String, io.soliton.protobuf.ClientMethod<? extends com.google.protobuf.Message>> methods = com.google.common.collect.ImmutableMap.<String, io.soliton.protobuf.ClientMethod<? extends com.google.protobuf.Message>>builder()\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("        .put(\"").append(method.getName()).append("\", new io.soliton.protobuf.ClientMethod<").append(types.lookup(method.getOutputType())).append(">() {\n");
			serviceFile.append("           public String serviceName() { return \"" + service.getName() + "\"; }\n");
			serviceFile.append("           public String name() { return \"").append(method.getName()).append("\"; }\n");
			serviceFile.append("           public com.google.protobuf.Parser<").append(types.lookup(method.getOutputType())).append("> outputParser() { return ").append(types.lookup(method.getOutputType())).append(".PARSER; }\n");
			serviceFile.append("})\n");
		}
		serviceFile.append("          .build();\n");
		serviceFile.append("    return new Interface() {\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("      public com.google.common.util.concurrent.ListenableFuture<").append(types.lookup(method.getOutputType())).append("> ").append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName())).append("(").append(types.lookup(method.getInputType())).append(" request) {\n");
			serviceFile.append("        return (com.google.common.util.concurrent.ListenableFuture<").append(types.lookup(method.getOutputType())).append(">) transport.encodeMethodCall(methods.get(\"").append(method.getName()).append("\"), request);\n");
			serviceFile.append("      }\n");
		}
		serviceFile.append("    };\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static io.soliton.protobuf.Service newService(final Interface implementation) {\n");
		serviceFile.append("    final com.google.common.collect.ImmutableMap<String, io.soliton.protobuf.ServerMethod<? extends com.google.protobuf.Message, ? extends com.google.protobuf.Message>> methods = com.google.common.collect.ImmutableMap.<String, io.soliton.protobuf.ServerMethod<? extends com.google.protobuf.Message, ? extends com.google.protobuf.Message>>builder()\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("      .put(\"").append(method.getName()).append("\", ").append(newMethodDeclaration(method)).append(")\n");
		}
		serviceFile.append("      .build();\n\n");
		
		serviceFile.append("    return new ").append(service.getName()).append("() {\n");
		
		serviceFile.append("      public io.soliton.protobuf.ServerMethod<? extends com.google.protobuf.Message, ? extends com.google.protobuf.Message> lookup(String name) {\n");
		serviceFile.append("        return methods.get(name);\n");
		serviceFile.append("      }\n\n");
		
		serviceFile.append("      public com.google.common.collect.ImmutableMap<String, io.soliton.protobuf.ServerMethod<? extends com.google.protobuf.Message, ? extends com.google.protobuf.Message>> methods() {\n");
		serviceFile.append("        return methods;\n");
		serviceFile.append("      }\n");serviceFile.append("    };\n\n");
		
		serviceFile.append("  }\n");
		serviceFile.append("}");
		CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
    CodeGeneratorResponse.File.Builder file = CodeGeneratorResponse.File.newBuilder();
    file.setContent(serviceFile.toString());
    file.setName(javaPackage.replace('.', '/') + '/' + service.getName() + ".java");
    if (!multipleFiles) {
      file.setName(javaPackage.replace('.', '/') + '/' + outerClassName + ".java");
      file.setInsertionPoint("outer_class_scope");
    }
    response.addFile(file);
    response.build().writeTo(output);
	}
	
	private String newMethodDeclaration(MethodDescriptorProto method) {
		String javaMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName());
		StringBuilder methodDeclaration = new StringBuilder();
		String inputType = types.lookup(method.getInputType()).toString();
		String outputType = types.lookup(method.getOutputType()).toString();
		methodDeclaration.append("new io.soliton.protobuf.ServerMethod<")
				.append(inputType).append(", ").append(outputType).append(">() {\n")
				.append("        public String name() { return \"").append(method.getName()).append("\"; }\n")
		    .append("        public com.google.protobuf.Parser<").append(inputType).append("> inputParser() { return ").append(inputType).append(".PARSER; }\n")
				.append("        public com.google.common.util.concurrent.ListenableFuture<").append(outputType).append("> invoke(").append(inputType).append(" request) { return implementation.").append(javaMethodName).append("(request); }\n")
				.append("      }");

    return methodDeclaration.toString();
	}
}
