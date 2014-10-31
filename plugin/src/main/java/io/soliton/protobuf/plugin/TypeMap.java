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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;

/**
 * Keeps a tab on the known protobuf message types and their associated Java
 * type.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TypeMap {

	private final ImmutableMap<String, JavaType> types;

	private TypeMap(ImmutableMap<String, JavaType> types) {
		this.types = types;
	}

	public static TypeMap of(FileDescriptorProto protoFile) {
		ImmutableMap.Builder<String, JavaType> types = ImmutableMap.builder();
		FileOptions options = protoFile.getOptions();

		String protoPackage = "." + (protoFile.hasPackage() ?
									 protoFile.getPackage() : "");
		String javaPackage = options.hasJavaPackage() ?
							 options.getJavaPackage() : protoFile.hasPackage() ?
														protoFile.getPackage() : null;
		String enclosingClass = options.getJavaMultipleFiles() ?
								null : options.hasJavaOuterClassname() ?
									   options.getJavaOuterClassname() : createOuterJavaClassname(
				protoFile.getName());

		for (DescriptorProto message : protoFile.getMessageTypeList()) {
			types.put(protoPackage + "." + message.getName(),
					new JavaType(javaPackage, enclosingClass, message.getName()));
		}

		return new TypeMap(types.build());
	}

	/**
	 * @param name
	 * @return
	 */
	@VisibleForTesting
	static String createOuterJavaClassname(String name) {
		if (name.endsWith(".proto")) {
			name = name.substring(0, name.length() - ".proto".length());
		}
		name = Iterables.getLast(Splitter.on('/').split(name));
		name = name.replace('-', '_');
		name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public JavaType lookup(String name) {
		return types.get(name);
	}

	public TypeMap mergeWith(TypeMap other) {
		return null;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("Types", types).toString();
	}
}
