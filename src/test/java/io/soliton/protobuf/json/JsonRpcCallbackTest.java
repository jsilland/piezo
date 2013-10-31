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

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests for {@link JsonRpcCallback}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class JsonRpcCallbackTest {

  @Test
  public void testOnSuccess() {
    JsonObject payload = new JsonObject();
    payload.addProperty("foo", "bar");
    JsonRpcResponse response = JsonRpcResponse.success(payload, new JsonPrimitive(2));
    Channel channel = Mockito.mock(Channel.class);

    JsonRpcCallback callback = new JsonRpcCallback(new JsonPrimitive(2), channel, false);
    callback.onSuccess(response);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(channel).writeAndFlush(captor.capture());

    Object captured = captor.getValue();
    Assert.assertTrue(captured instanceof FullHttpResponse);
    FullHttpResponse httpResponse = (FullHttpResponse) captured;
    Assert.assertEquals("application/json",
        httpResponse.headers().get(HttpHeaders.Names.CONTENT_TYPE));

    JsonElement responseElement = new JsonParser().parse(
        httpResponse.content().toString(Charsets.UTF_8));

    Assert.assertEquals("bar",
        JsonRpcResponse.fromJson((JsonObject) responseElement).result().get("foo").getAsString());
  }

  @Test
  public void testOnFailure() {
    Channel channel = Mockito.mock(Channel.class);

    JsonRpcCallback callback = new JsonRpcCallback(new JsonPrimitive(2), channel, false);
    callback.onFailure(new Exception("Uh oh"));

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(channel).writeAndFlush(captor.capture());

    Object captured = captor.getValue();
    Assert.assertTrue(captured instanceof FullHttpResponse);
    FullHttpResponse httpResponse = (FullHttpResponse) captured;
    Assert.assertEquals("application/json",
        httpResponse.headers().get(HttpHeaders.Names.CONTENT_TYPE));

    JsonElement responseElement = new JsonParser().parse(
        httpResponse.content().toString(Charsets.UTF_8));

    JsonRpcError error = JsonRpcResponse.fromJson((JsonObject) responseElement).error();
    Assert.assertTrue(error.getMessage().contains("Uh oh"));
    Assert.assertEquals(500, error.status().code());
  }
}
