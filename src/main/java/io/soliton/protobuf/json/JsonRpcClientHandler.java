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
import com.google.common.collect.MapMaker;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Client handler in charge of decoding the server's response and dispatching
 * it to the relevant client.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcClientHandler extends SimpleChannelInboundHandler<HttpResponse> {

  private static final Logger logger = Logger.getLogger(
      JsonRpcClientHandler.class.getCanonicalName());
  private static final Random RANDOM = new Random();

  private final ConcurrentMap<Long, JsonResponseFuture<? extends Message>> inFlightRequests =
      new MapMaker().makeMap();

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext,
        HttpResponse response) throws Exception {
    if (!(response instanceof HttpContent)) {
      logger.severe("Returned response has no content");
      return;
    }

    HttpContent content = (HttpContent) response;

    if (!response.headers().get(HttpHeaders.Names.CONTENT_TYPE)
        .equals(JsonRpcProtocol.CONTENT_TYPE)) {
      logger.warning(
          "Incorrect Content-Type: " + response.headers().get(HttpHeaders.Names.CONTENT_TYPE));
    }

    JsonElement root;
    try {
      ByteBufInputStream stream = new ByteBufInputStream(content.content());
      JsonReader reader = new JsonReader(new InputStreamReader(stream, Charsets.UTF_8));
      root = new JsonParser().parse(reader);
    } catch (JsonSyntaxException jsonException) {
      logger.warning("JSON response cannot be decoded");
      return;
    }
    if (!root.isJsonObject()) {
      logger.warning("JSON response is not a JSON object: " + root.toString());
      return;
    }

    JsonRpcResponse jsonRpcResponse = JsonRpcResponse.fromJson(root.getAsJsonObject());

    JsonElement requestId = jsonRpcResponse.id();
    if (requestId == null || !requestId.isJsonPrimitive()) {
      logger.warning("Received response identifier is not JSON primitive: "
          + requestId.toString());
      return;
    }

    JsonResponseFuture<? extends Message> future = inFlightRequests.remove(requestId.getAsLong());
    if (future == null) {
      logger.warning("Response received for unknown identifier: " + requestId.getAsLong());
      return;
    }

    future.setResponse(jsonRpcResponse);
  }

  /**
   * Returns a new provisional response suited to receive results of a given
   * protobuf message type.
   *
   * @param outputBuilder the builder for the message's type
   * @param <O> the type of the message this response will handle
   */
  <O extends Message> JsonResponseFuture<O> newProvisionalResponse(
      Message.Builder outputBuilder) {
    long requestId = RANDOM.nextLong();
    JsonResponseFuture<O> outputFuture = new JsonResponseFuture<>(requestId, outputBuilder);
    inFlightRequests.put(requestId, outputFuture);
    return outputFuture;
  }

  /**
   * Signals to this handler to release any resource associated with the
   * given request identifier.
   *
   * @param requestId the request identifier
   */
  void finish(long requestId) {
    inFlightRequests.remove(requestId);
  }
}
