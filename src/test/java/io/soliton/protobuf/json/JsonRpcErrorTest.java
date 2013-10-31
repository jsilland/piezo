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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link JsonRpcError}.
 */
public class JsonRpcErrorTest {

  @Test
  public void testAccessors() {
    JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST, "Dude !");
    Assert.assertEquals(HttpResponseStatus.BAD_REQUEST, error.status());
    Assert.assertEquals("Dude !", error.getMessage());
  }

  @Test
  public void testToJson() {
    JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST, "Dude !");
    Assert.assertEquals(400, error.toJson().get("code").getAsInt());
    Assert.assertEquals("Dude !", error.toJson().get("message").getAsString());
  }

  @Test
  public void testFromJson() {
    JsonElement element = new JsonParser().parse("{\"code\": 400, \"message\": \"Dude !\"}");
    JsonRpcError error = JsonRpcError.fromJson(element.getAsJsonObject());
    Assert.assertEquals(HttpResponseStatus.BAD_REQUEST, error.status());
    Assert.assertEquals("Dude !", error.getMessage());
  }
}
