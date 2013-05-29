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
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;

import java.io.IOException;

/**
 *
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class ProtoFileHandler {

	private final TypeMap types;
	
	public ProtoFileHandler(TypeMap types) {
		this.types = Preconditions.checkNotNull(types);
	}
	
	public void handle(FileDescriptorProto protoFile) throws IOException {
		String javaPackage = inferJavaPackage(protoFile);
		ProtoServiceHandler serviceHandler = new ProtoServiceHandler(javaPackage, types);
		for (ServiceDescriptorProto service : protoFile.getServiceList()) {
			serviceHandler.handle(service);
		}
	}
	
	public static String inferJavaPackage(FileDescriptorProto file) {
		FileOptions options = file.getOptions();
		return options.hasJavaPackage() ?
				options.getJavaPackage() : file.hasPackage() ? file.getPackage() : null;
	}
}
