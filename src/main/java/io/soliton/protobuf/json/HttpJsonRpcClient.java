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
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.GenericFutureListener;
import io.soliton.protobuf.ChannelInitializers;
import io.soliton.protobuf.Client;
import io.soliton.protobuf.ClientMethod;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

/**
 * Implementation of an RPC client that encodes method calls using the JSON-RPC
 * protocol over an HTTP transport.
 *
 * @see <a href="http://json-rpc.org/">JSON-RPC</a>
 * @author Julien Silland (julien@soliton.io)
 */
public class HttpJsonRpcClient implements Client {

  private static final Logger logger = Logger.getLogger(
      HttpJsonRpcClient.class.getCanonicalName());
  private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .generateNonExecutableJson()
      .create();

  private final Channel channel;
  private final JsonRpcClientHandler handler;
  private final String rpcPath;

  /**
   * Exhaustive constructor.
   *
   * <p>This constructor connects synchronously to the specified server.</p>
   *
   * @param remoteAddress the address of the remote server
   * @param rpcPath the path of the RPC endpoint on the remote server
   */
  public static HttpJsonRpcClient to(HostAndPort remoteAddress, String rpcPath) throws IOException {
    Preconditions.checkNotNull(remoteAddress);
    Preconditions.checkNotNull(rpcPath);
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(new NioEventLoopGroup());
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    bootstrap.channel(NioSocketChannel.class);
    JsonRpcClientHandler handler = new JsonRpcClientHandler();
    bootstrap.handler(ChannelInitializers.httpClient(handler));

    ChannelFuture future = bootstrap.connect(remoteAddress.getHostText(), remoteAddress.getPort());
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      logger.info("Piezo client successfully connected to " + remoteAddress.toString());
    } else {
      logger.warning("Piezo client failed to connect to " + remoteAddress.toString());
      throw new IOException(future.cause());
    }
    return new HttpJsonRpcClient(future.channel(), handler, rpcPath);
  }

  /**
   * Exhaustive constructor.
   *
   * <p>This constructor connects synchronously to the specified server.</p>
   *
   * @param channel the connection to the remote server
   * @param handler the hand;er in charge of receiving server responses
   * @param rpcPath the path of the RPC endpoint on the remote server
   */
  public HttpJsonRpcClient(Channel channel, JsonRpcClientHandler handler,
      String rpcPath) {
    this.channel = channel;
    this.handler = handler;
    this.rpcPath = rpcPath;
  }

  @Override
  public <O extends Message> ListenableFuture<O> encodeMethodCall(ClientMethod<O> method,
      Message input) {
    final JsonResponseFuture<O> responseFuture =
        handler.newProvisionalResponse(method.outputBuilder());

    JsonObject request = new JsonRpcRequest(method.serviceName(), method.name(),
        new JsonPrimitive(responseFuture.requestId()), Messages.toJson(input)).toJson();

    ByteBuf requestBuffer = Unpooled.buffer();
    JsonWriter writer = new JsonWriter(
        new OutputStreamWriter(new ByteBufOutputStream(requestBuffer), Charsets.UTF_8));
    GSON.toJson(request, writer);
    try {
      writer.flush();
    } catch (IOException ioe) {
      // Deliberately ignored, as this doesn't involve any I/O
    }
    QueryStringEncoder encoder = new QueryStringEncoder(rpcPath);
    encoder.addParam("pp", "0");
    HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
        encoder.toString(), requestBuffer);
    httpRequest.headers().set(HttpHeaders.Names.CONTENT_TYPE, JsonRpcProtocol.CONTENT_TYPE);
    httpRequest.headers().set(HttpHeaders.Names.CONTENT_LENGTH, requestBuffer.readableBytes());

    channel.writeAndFlush(httpRequest).addListener(new GenericFutureListener<ChannelFuture>() {

      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          handler.finish(responseFuture.requestId());
          responseFuture.setException(future.cause());
        }
      }

    });

    return responseFuture;
  }
}
