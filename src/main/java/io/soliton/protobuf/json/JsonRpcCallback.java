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
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Implements the logic executed upon a service method returning a result or
 * throwing an exception.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcCallback implements FutureCallback<JsonRpcResponse> {

  private static final Gson GSON_PP = new GsonBuilder()
      .disableHtmlEscaping()
      .generateNonExecutableJson()
      .setPrettyPrinting()
      .create();
  private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .generateNonExecutableJson()
      .create();


  private final JsonElement id;
  private final Channel channel;
  private final boolean prettyPrint;

  /**
   * Exhaustive constructor.
   *
   * @param id the identifier of the request, as sent by the client
   * @param channel the channel on which the communication is taking place
   * @param prettyPrint determines whether the output should be pretty-printed
   */
  public JsonRpcCallback(JsonElement id, Channel channel, boolean prettyPrint) {
    this.id = id;
    this.channel = channel;
    this.prettyPrint = prettyPrint;
  }

  @Override
  public void onSuccess(JsonRpcResponse response) {
    ByteBuf responseBuffer = Unpooled.buffer();
    JsonWriter writer = new JsonWriter(
        new OutputStreamWriter(new ByteBufOutputStream(responseBuffer), Charsets.UTF_8));
    (prettyPrint ? GSON_PP : GSON).toJson(response.toJson(), writer);
    try {
      writer.flush();
    } catch (IOException ioe) {
      // Deliberately ignored, no I/O is involved
    }
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK, responseBuffer);
    httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
    httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, responseBuffer.readableBytes());
    channel.writeAndFlush(httpResponse);
  }

  @Override
  public void onFailure(Throwable t) {
    JsonRpcError error = new JsonRpcError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
        t.getMessage());
    JsonRpcResponse response = JsonRpcResponse.error(error, id);
    onSuccess(response);
  }
}
