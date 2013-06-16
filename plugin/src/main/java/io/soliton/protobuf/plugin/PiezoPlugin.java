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

import com.google.common.base.Preconditions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Main class of the Piezo plugin.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class PiezoPlugin {

	private final CodeGeneratorRequest request;
  private final OutputStream output;
	
	public PiezoPlugin(CodeGeneratorRequest request, OutputStream output) {
		this.request = Preconditions.checkNotNull(request);
    this.output = Preconditions.checkNotNull(output);
	}
	
	public void run() throws IOException {
		for (FileDescriptorProto file : request.getProtoFileList()) {
			new ProtoFileHandler(TypeMap.of(file), output).handle(file);
		}
	}
	
	public static void main(String... args) throws IOException {
		CodeGeneratorRequest request = CodeGeneratorRequest.newBuilder()
        .mergeFrom(System.in).build();
		new PiezoPlugin(request, System.out).run();
	}
}
