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

import io.soliton.protobuf.Server;
import io.soliton.protobuf.ServerLogger;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Netty handler implementing the JSON RPC protocol over HTTP.
 *
 * @author Julien Silland (julien@soliton.io)
 */
final class JsonRpcServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private static final String PRETTY_PRINT_PARAMETER = "prettyPrint";
  private static final String PP_PARAMETER = "pp";

  private final Server server;
  private final String rpcPath;
  private final ServerLogger serverLogger;
  private final ExecutorService responseCallbackExecutor = Executors.newCachedThreadPool();

  /**
   * Exhaustive constructor.
   *
   * @param server the server to which this handler is attached
   * @param rpcPath the HTTP endpoint path
   * @param serverLogger the object to log server operations to
   */
  public JsonRpcServerHandler(Server server, String rpcPath, ServerLogger serverLogger) {
    this.server = server;
    this.rpcPath = rpcPath;
    this.serverLogger = serverLogger;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
    if (!(request instanceof HttpContent)) {
      JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "HTTP request was empty");
      JsonRpcResponse response = JsonRpcResponse.error(error);
      new JsonRpcCallback(null, ctx.channel(), true).onSuccess(response);
      return;
    }

    HttpContent content = (HttpContent) request;

    JsonElement root;
    try {
      root = new JsonParser().parse(
          new InputStreamReader(new ByteBufInputStream(content.content()), Charsets.UTF_8));
    } catch (JsonSyntaxException jse) {
      JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Cannot decode JSON payload");
      JsonRpcResponse response = JsonRpcResponse.error(error);
      new JsonRpcCallback(null, ctx.channel(), true).onSuccess(response);
      return;
    }

    JsonElement id;
    if (!root.isJsonObject()) {
      JsonRpcResponse response = JsonRpcResponse.error(
          new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
              "Received payload is not a JSON Object"));
      new JsonRpcCallback(null, ctx.channel(), true).onSuccess(response);
      return;
    } else {
      id = root.getAsJsonObject().get(JsonRpcProtocol.ID);
    }

    JsonRpcError transportError = validateTransport(request);
    if (transportError != null) {
      JsonRpcResponse response = JsonRpcResponse.error(transportError, id);
      new JsonRpcCallback(id, ctx.channel(), true).onSuccess(response);
      return;
    }

    JsonRpcRequest jsonRpcRequest;
    try {
      jsonRpcRequest = JsonRpcRequest.fromJson(root);
    } catch (JsonRpcError error) {
      JsonRpcResponse response = JsonRpcResponse.error(error, id);
      new JsonRpcCallback(null, ctx.channel(), true).onSuccess(response);
      return;
    }

    Futures.addCallback(jsonRpcRequest.invoke(server.serviceGroup()),
        new JsonRpcCallback(jsonRpcRequest.id(), ctx.channel(), shouldPrettyPrint(request)),
        responseCallbackExecutor);
  }

  /**
   * In charge of validating all the transport-related aspects of the incoming
   * HTTP request.
   * <p/>
   * <p>The checks include:</p>
   * <p/>
   * <ul>
   * <li>that the request's path matches that of this handler;</li>
   * <li>that the request's method is {@code POST};</li>
   * <li>that the request's content-type is {@code application/json};</li>
   * </ul>
   *
   * @param request the received HTTP request
   * @return {@code null} if the request passes the transport checks, an error
   *         to return to the client otherwise.
   * @throws URISyntaxException if the URI of the request cannot be parsed
   */
  private JsonRpcError validateTransport(HttpRequest request) throws URISyntaxException,
      JsonRpcError {
    URI uri = new URI(request.getUri());
    JsonRpcError error = null;

    if (!uri.getPath().equals(rpcPath)) {
      error = new JsonRpcError(HttpResponseStatus.NOT_FOUND, "Not Found");
    }

    if (!request.getMethod().equals(HttpMethod.POST)) {
      error = new JsonRpcError(HttpResponseStatus.METHOD_NOT_ALLOWED,
          "Method not allowed");
    }

    if (!request.headers().get(HttpHeaders.Names.CONTENT_TYPE)
        .equals(JsonRpcProtocol.CONTENT_TYPE)) {
      error = new JsonRpcError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE,
          "Unsupported media type");
    }

    return error;
  }

  /**
   * Determines whether the response to the request should be pretty-printed.
   *
   * @param request the HTTP request.
   * @return {@code true} if the response should be pretty-printed.
   */
  private boolean shouldPrettyPrint(HttpRequest request) {
    QueryStringDecoder decoder = new QueryStringDecoder(request.getUri(), Charsets.UTF_8, true, 2);
    Map<String, List<String>> parameters = decoder.parameters();
    if (parameters.containsKey(PP_PARAMETER)) {
      return parseBoolean(parameters.get(PP_PARAMETER).get(0));
    } else if (parameters.containsKey(PRETTY_PRINT_PARAMETER)) {
      return parseBoolean(parameters.get(PRETTY_PRINT_PARAMETER).get(0));
    }
    return true;
  }

  /**
   * Determines the 'truthiness' of a string value.
   *
   * @param value
   * @return
   */
  private boolean parseBoolean(String value) {
    return "1".equals(value) || "true".equals(value);
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
