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

import io.soliton.protobuf.plugin.testing.TestingMultiFile;
import io.soliton.protobuf.plugin.testing.TestingOneFile;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TypeMap}.
 */
public class TypeMapTest {

  @Test
  public void testCreateAndLookupSingleFile() {
    TypeMap types = TypeMap.of(TestingOneFile.getDescriptor().toProto());
    JavaType type = types.lookup(".soliton.piezo.testing.Person");
    Assert.assertNotNull(type);
    Assert.assertEquals("io.soliton.protobuf.plugin.testing.TestingOneFile.Person",
        type.toString());
  }

  @Test
  public void testCreateAndLookupMultipleFiles() {
    TypeMap types = TypeMap.of(TestingMultiFile.getDescriptor().toProto());
    JavaType type = types.lookup(".soliton.piezo.testing.SearchRequest");
    Assert.assertNotNull(type);
    Assert.assertEquals("io.soliton.protobuf.plugin.testing.SearchRequest",
        type.toString());
  }
}
