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

package io.soliton.protobuf.json;

import io.soliton.protobuf.testing.All;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Messages}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TestMessages {

  @Test
  public void testRoundtripSingleFields() throws Exception {
    All original = All.newBuilder()
        .setString("string")
        .setDouble(0.1)
        .setFloat(.6f)
        .setInt32(42)
        .setInt64(32L)
        .setUint32(64)
        .setUint64(128)
        .setSint32(256)
        .setSint64(512)
        .setFixed32(1024)
        .setFixed64(2048)
        .setSfixed32(4096)
        .setSfixed64(8192)
        .setBool(false)
        .setBytes(ByteString.copyFrom("你好".getBytes(Charsets.UTF_8)))
        .setMessage(All.newBuilder().setString("string2"))
        .setFoo(All.Foo.BAR)
        .build();

    JsonObject json = Messages.toJson(original);

    All copy = (All) Messages.fromJson(All.newBuilder(), json);
    Assert.assertEquals(copy, original);
  }

  @Test
  public void testRoundtriprepeatedFields() throws Exception {
    All original = All.newBuilder()
        .addRepeatedString("string")
        .addRepeatedDouble(0.1)
        .addRepeatedFloat(.6f)
        .addRepeatedInt32(42)
        .addRepeatedInt64(32L)
        .addRepeatedUint32(64)
        .addRepeatedUint64(128)
        .addRepeatedSint32(256)
        .addRepeatedSint64(512)
        .addRepeatedFixed32(1024)
        .addRepeatedFixed64(2048)
        .addRepeatedSfixed32(4096)
        .addRepeatedSfixed64(8192)
        .addRepeatedBool(false)
        .addRepeatedBytes(ByteString.copyFrom("你好".getBytes(Charsets.UTF_8)))
        .addRepeatedMessage(All.newBuilder().setString("string2"))
        .addRepeatedFoo(All.Foo.BAR)
        .build();

    JsonObject json = Messages.toJson(original);

    All copy = (All) Messages.fromJson(All.newBuilder(), json);
    Assert.assertEquals(copy, original);
  }
}
