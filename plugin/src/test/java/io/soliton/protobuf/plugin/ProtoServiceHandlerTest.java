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

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import io.soliton.protobuf.plugin.testing.Testing;
import org.junit.Assert;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ProtoServiceHandlerTest {

  private static class JavaSourceFromString extends SimpleJavaFileObject {

    final String code;

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
    // Triggering code generation
    Descriptors.Descriptor personDescriptor = Testing.Person.getDescriptor();
    DescriptorProtos.FileDescriptorProto protoFile = personDescriptor.getFile().toProto();
    TypeMap types = TypeMap.of(protoFile);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ProtoServiceHandler serviceHandler = new ProtoServiceHandler(
        "io.soliton.protobuf.plugin.testing", types, output);
    serviceHandler.handle(protoFile.getServiceList().get(0));

    // Parsing code generation result
    PluginProtos.CodeGeneratorResponse response = PluginProtos.CodeGeneratorResponse.parseFrom(
        output.toByteArray());
    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.getFileCount());
    Assert.assertEquals("io/soliton/protobuf/plugin/testing/PhoneBook.java",
        response.getFile(0).getName());
    String fileContent = response.getFile(0).getContent();

    // Extracting Java source code and initiating parsing
    JavaFileObject file = new JavaSourceFromString
        ("io/soliton/protobuf/plugin/testing/PhoneBook", fileContent);
    JavacTool compiler = JavacTool.create();
    StringWriter errorOutput = new StringWriter();
    JavaFileManager fileManager = compiler.getStandardFileManager(null, null, Charsets.UTF_8);
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    List<String> compilerOptions = Lists.newArrayList("-classpath",
        System.getProperty("java.class.path"));
    List<JavaFileObject> compilationUnits = Lists.newArrayList(file);
    JavacTask compilationTask = compiler.getTask(errorOutput, fileManager, diagnostics,
        compilerOptions, null, compilationUnits);
    Iterable<? extends CompilationUnitTree> compiledUnits = compilationTask.parse();

    // Checking parsing results
    Assert.assertEquals(1, Iterables.size(compiledUnits));
    CompilationUnitTree compiledUnit = compiledUnits.iterator().next();
    ExpressionTree packageName = compiledUnit.getPackageName();
    Assert.assertEquals("io.soliton.protobuf.plugin.testing", packageName.toString());
    List<? extends Tree> declaredTypes = compiledUnit.getTypeDecls();
    Assert.assertEquals(1, declaredTypes.size());
    Tree rootType = declaredTypes.get(0);
    Assert.assertEquals(Tree.Kind.CLASS, rootType.getKind());

    // Checking root class properties
    ClassTree rootClass = (ClassTree) rootType;
    Assert.assertTrue(rootClass.getModifiers().getFlags().contains(Modifier.PUBLIC));
    Assert.assertTrue(rootClass.getModifiers().getFlags().contains(Modifier.ABSTRACT));
    Assert.assertTrue(rootClass.getSimpleName().contentEquals("PhoneBook"));
    Assert.assertEquals(1, rootClass.getImplementsClause().size());
    Assert.assertEquals(Tree.Kind.IDENTIFIER, rootClass.getImplementsClause().get(0).getKind());
    Assert.assertTrue(((IdentifierTree) rootClass.getImplementsClause().get(0))
        .getName().contentEquals("Service"));

    // Checking root class methods
    Map<String, MethodTree> methods = Maps.newHashMap();
    ClassTree interfaceClass = null;
    for (Tree member : rootClass.getMembers()) {
      if (Tree.Kind.METHOD == member.getKind()) {
        MethodTree method = (MethodTree) member;
        methods.put(method.getName().toString(), method);
      } else if (Tree.Kind.CLASS == member.getKind()) {
        interfaceClass = (ClassTree) member;
      }
    }

    Assert.assertTrue(methods.containsKey("newStub"));
    MethodTree newStub = methods.get("newStub");
    Assert.assertTrue(((IdentifierTree) newStub.getReturnType()).getName()
        .contentEquals("Interface"));
    Assert.assertTrue(((IdentifierTree) newStub.getParameters().get(0).getType()).getName()
        .contentEquals("Client"));

    Assert.assertTrue(methods.containsKey("newService"));
    MethodTree newService = methods.get("newService");
    Assert.assertTrue(((IdentifierTree) newService.getReturnType()).getName()
        .contentEquals("Service"));
    Assert.assertTrue(((IdentifierTree) newService.getParameters().get(0).getType()).getName()
        .contentEquals("Interface"));

    // Checking interface methods
    Assert.assertNotNull(interfaceClass);
    Map<String, MethodTree> interfaceMethods = Maps.newHashMap();
    Assert.assertTrue(interfaceClass.getSimpleName().contentEquals("Interface"));
    for (Tree member : interfaceClass.getMembers()) {
      if (Tree.Kind.METHOD == member.getKind()) {
        MethodTree method = (MethodTree) member;
        interfaceMethods.put(method.getName().toString(), method);
      }
    }

    Assert.assertTrue(interfaceMethods.containsKey("lookup"));
    MethodTree lookup = interfaceMethods.get("lookup");
    Assert.assertEquals(Tree.Kind.PARAMETERIZED_TYPE, lookup.getReturnType().getKind());
    ParameterizedTypeTree returnTypeTree = (ParameterizedTypeTree) lookup.getReturnType();
    Assert.assertTrue(((IdentifierTree) returnTypeTree.getType()).getName()
        .contentEquals("ListenableFuture"));
    Assert.assertTrue(((MemberSelectTree) returnTypeTree.getTypeArguments().get(0)).getIdentifier()
        .toString().contains("PhoneNumber"));
  }
}
