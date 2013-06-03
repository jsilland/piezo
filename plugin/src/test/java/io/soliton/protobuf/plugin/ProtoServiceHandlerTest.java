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

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;
import io.soliton.protobuf.plugin.testing.Testing;
import org.junit.Assert;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URI;

public class ProtoServiceHandlerTest {

  private static class JavaSourceFromString extends SimpleJavaFileObject {

    final String code;

    /**
     * Constructs a new JavaSourceFromString.
     * @param name the name of the compilation unit represented by this file object
     * @param code the source code for the compilation unit represented by this file object
     */
    JavaSourceFromString(String name, String code) {
      super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
          Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return code;
    }
  }

  @Test
  public void testHandle() throws Exception {
    Descriptors.Descriptor personDescriptor = Testing.Person.getDescriptor();
    DescriptorProtos.FileDescriptorProto protoFile = personDescriptor.getFile().toProto();
    TypeMap types = TypeMap.of(protoFile);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ProtoServiceHandler serviceHandler = new ProtoServiceHandler(
        "io.soliton.protobuf.plugin.testing", types, output);
    serviceHandler.handle(protoFile.getServiceList().get(0));
    PluginProtos.CodeGeneratorResponse response = PluginProtos.CodeGeneratorResponse.parseFrom(
        output.toByteArray());
    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.getFileCount());
    Assert.assertEquals("io/soliton/protobuf/plugin/testing/PhoneBookService.java",
        response.getFile(0).getName());
    String fileContent = response.getFile(0).getContent();
    JavaFileObject file = new JavaSourceFromString
        ("io/soliton/protobuf/plugin/testing/PhoneBookService.java", fileContent);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
  }
}
