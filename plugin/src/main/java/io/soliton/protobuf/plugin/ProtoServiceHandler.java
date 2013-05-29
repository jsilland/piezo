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

/**
 *
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class ProtoServiceHandler {

	private final String javaPackage;
	private final TypeMap types;
	
	public ProtoServiceHandler(String javaPackage, TypeMap types) {
		this.javaPackage = javaPackage;
		this.types = Preconditions.checkNotNull(types);
	}
	
	public void handle(ServiceDescriptorProto service) throws IOException {
		StringBuilder serviceFile = new StringBuilder();
		serviceFile.append("// Generated code. DO NOT EDIT!\n\n");
		if (javaPackage != null) {
			serviceFile.append("package ").append(javaPackage).append(";\n\n");
		}
		
		serviceFile.append("import io.soliton.protobuf.ClientMethod;\n");
		serviceFile.append("import io.soliton.protobuf.Client;\n");
		serviceFile.append("import io.soliton.protobuf.ServerMethod;\n");
		serviceFile.append("import io.soliton.protobuf.Service;\n");
		serviceFile.append("import com.google.common.collect.ImmutableMap;\n");
		serviceFile.append("import com.google.common.util.concurrent.ListenableFuture;\n");
		serviceFile.append("import com.google.protobuf.Message;\n");
		serviceFile.append("import com.google.protobuf.Parser;\n\n");
		
		serviceFile.append(String.format("public abstract class %s implements Service {\n", service.getName()));
		
		serviceFile.append("  public String shortName() {\n");
		serviceFile.append("    return \"").append(service.getName()).append("\";\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public String fullName() {\n");
		serviceFile.append("    return \"").append(service.getName()).append("\";\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static interface Interface {\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			String javaMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName());
			serviceFile.append("    public ListenableFuture<").append(types.lookup(method.getOutputType())).append("> ").append(javaMethodName).append("(").append(types.lookup(method.getInputType())).append(" request);\n");
		}
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static Interface newStub(final Client transport) {\n");
		serviceFile.append("    final ImmutableMap<String, ClientMethod<? extends Message>> methods = ImmutableMap.<String, ClientMethod<? extends Message>>builder()\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("        .put(\"").append(method.getName()).append("\", new ClientMethod<").append(types.lookup(method.getOutputType())).append(">() {\n");
			serviceFile.append("           public String serviceName() { return \"" + service.getName() + "\"; }\n");
			serviceFile.append("           public String name() { return \"").append(method.getName()).append("\"; }\n");
			serviceFile.append("           public Parser<").append(types.lookup(method.getOutputType())).append("> outputParser() { return ").append(types.lookup(method.getOutputType())).append(".PARSER; }\n");
			serviceFile.append("})\n");
		}
		serviceFile.append("          .build();\n");
		serviceFile.append("    return new Interface() {\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("      public ListenableFuture<").append(types.lookup(method.getOutputType())).append("> ").append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName())).append("(").append(types.lookup(method.getInputType())).append(" request) {\n");
			serviceFile.append("        return (ListenableFuture<").append(types.lookup(method.getOutputType())).append(">) transport.encodeMethodCall(methods.get(\"").append(method.getName()).append("\"), request);\n");
			serviceFile.append("      }\n");
		}
		serviceFile.append("    };\n");
		serviceFile.append("  }\n\n");
		
		serviceFile.append("  public static Service newService(final Interface i) {\n");
		serviceFile.append("    final ImmutableMap<String, ServerMethod<? extends Message, ? extends Message>> methods = ImmutableMap.<String, ServerMethod<? extends Message, ? extends Message>>builder()\n");
		for (MethodDescriptorProto method : service.getMethodList()) {
			serviceFile.append("      .put(\"").append(method.getName()).append("\", ").append(newMethodDeclaration(method)).append(")\n");
		}
		serviceFile.append("      .build();\n\n");
		
		serviceFile.append("    return new ").append(service.getName()).append("() {\n");
		
		serviceFile.append("      public ServerMethod<? extends Message, ? extends Message> lookup(String name) {\n");
		serviceFile.append("        return methods.get(name);\n");
		serviceFile.append("      }\n\n");
		
		serviceFile.append("      public ImmutableMap<String, ServerMethod<? extends Message, ? extends Message>> methods() {\n");
		serviceFile.append("        return methods;\n");
		serviceFile.append("      }\n");serviceFile.append("    };\n\n");
		
		serviceFile.append("  }\n");
		serviceFile.append("}");
		CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
		response.addFileBuilder().setContent(serviceFile.toString())	
				.setName(javaPackage.replace('.', '/') + '/' + service.getName() + ".java")
				.build();
		response.build().writeTo(System.out);
	}
	
	private String newMethodDeclaration(MethodDescriptorProto method) {
		String javaMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName());
		StringBuilder methodDeclaration = new StringBuilder();
		String inputType = types.lookup(method.getInputType()).toString();
		String outputType = types.lookup(method.getOutputType()).toString();
		methodDeclaration.append("new ServerMethod<")
				.append(inputType).append(", ").append(outputType).append(">() {\n")
				.append("        public String name() { return \"").append(method.getName()).append("\"; }\n")
		    .append("        public Parser<").append(inputType).append("> inputParser() { return ").append(inputType).append(".PARSER; }\n")
				.append("        public ListenableFuture<").append(outputType).append("> invoke(").append(inputType).append(" request) { return i.").append(javaMethodName).append("(request); }\n")
				.append("      }");

    return methodDeclaration.toString();
	}
}
