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
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import org.mvel2.templates.TemplateRuntime;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is in charge of generating the code of the concrete service
 * implementation.
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
    ImmutableList.Builder<ServiceHandlerData.Method> methods = ImmutableList.builder();
    for (MethodDescriptorProto method : service.getMethodList()) {
      ServiceHandlerData.Method methodData = new ServiceHandlerData.Method(
          method.getName(),
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.getName()),
          types.lookup(method.getInputType()).toString(),
          types.lookup(method.getOutputType()).toString());
      methods.add(methodData);
    }

    ServiceHandlerData.Service serviceData = new ServiceHandlerData.Service(
        service.getName(), methods.build());
    ServiceHandlerData data = new ServiceHandlerData(javaPackage, multipleFiles, serviceData);

    String template = Resources.toString(Resources.getResource(this.getClass(),
        "service_class.mvel"), Charsets.UTF_8);
    String serviceFile = (String) TemplateRuntime.eval(template,
        ImmutableMap.<String, Object>of("handler",  data));

		CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
    CodeGeneratorResponse.File.Builder file = CodeGeneratorResponse.File.newBuilder();
    file.setContent(serviceFile);
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
