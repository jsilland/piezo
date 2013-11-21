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
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Julien Silland (julien@soliton.io)
 */
public class ProtoFileHandler {

  private final TypeMap types;
  private final OutputStream output;

  public ProtoFileHandler(TypeMap types, OutputStream output) {
    this.types = Preconditions.checkNotNull(types);
    this.output = Preconditions.checkNotNull(output);
  }

  public void handle(FileDescriptorProto protoFile) throws IOException {
    String javaPackage = inferJavaPackage(protoFile);
    boolean multipleFiles = protoFile.getOptions().getJavaMultipleFiles();
    String outerClassName = null;
    if (!multipleFiles) {
      if (protoFile.getOptions().hasJavaOuterClassname()) {
        outerClassName = protoFile.getOptions().getJavaOuterClassname();
      } else {
        outerClassName = inferOuterClassName(protoFile);
      }
    }
    ProtoServiceHandler serviceHandler = new ProtoServiceHandler(javaPackage, types,
        multipleFiles, outerClassName, protoFile.getPackage(), output);
    for (ServiceDescriptorProto service : protoFile.getServiceList()) {
      serviceHandler.handle(service);
    }
  }

  @VisibleForTesting
  static String inferJavaPackage(FileDescriptorProto file) {
    FileOptions options = file.getOptions();
    return options.hasJavaPackage() ?
        options.getJavaPackage() : file.hasPackage() ? file.getPackage() : null;
  }

  @VisibleForTesting
  static String inferOuterClassName(FileDescriptorProto file) {
    String fileName = file.getName();
    if (fileName.endsWith(".proto")) {
      fileName = fileName.substring(0, fileName.length() - ".proto".length());
    }
    fileName = Iterables.getLast(Splitter.on('/').split(fileName));
    fileName = fileName.replace('-', '_');
    fileName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileName);
    return Character.toUpperCase(fileName.charAt(0)) + fileName.substring(1);
  }
}
