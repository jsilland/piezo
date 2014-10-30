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

import com.google.common.collect.ImmutableList;

public class ServiceHandlerData {

	public static class Service {

		private final String name;
		private final String fullName;
		private final ImmutableList<Method> methods;

		Service(String name, String fullName, ImmutableList<Method> methods) {
			this.name = name;
			this.fullName = fullName;
			this.methods = methods;
		}

		public String getName() {
			return name;
		}

		public String getFullName() {
			return fullName;
		}

		public ImmutableList<Method> getMethods() {
			return methods;
		}
	}

	public static class Method {

		private final String name;
		private final String javaName;
		private final String inputType;
		private final String outputType;

		Method(String name, String javaName, String inputType, String outputType) {
			this.name = name;
			this.javaName = javaName;
			this.inputType = inputType;
			this.outputType = outputType;
		}

		public String getName() {
			return name;
		}

		public String getJavaName() {
			return javaName;
		}

		public String getInputType() {
			return inputType;
		}

		public String getOutputType() {
			return outputType;
		}
	}

	private final String javaPackage;
	private final boolean multipleFiles;
	private final Service service;

	public ServiceHandlerData(String javaPackage, boolean multipleFiles, Service service) {
		this.javaPackage = javaPackage;
		this.multipleFiles = multipleFiles;
		this.service = service;
	}

	public String getJavaPackage() {
		return javaPackage;
	}

	public boolean getMultipleFiles() {
		return multipleFiles;
	}

	public Service getService() {
		return service;
	}
}
