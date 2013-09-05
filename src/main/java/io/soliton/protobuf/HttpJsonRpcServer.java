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

package io.soliton.protobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.logging.Logger;

/**
 * Concrete {@link Server} implementation surfacing the registered services
 * over an HTTP transport using the JSON-RPC protocol.
 *
 * @see <a href="http://json-rpc.org/">JSON-RPC</a>
 */
public class HttpJsonRpcServer implements Server {

  private static final Logger logger = Logger.getLogger(
      HttpJsonRpcServer.class.getCanonicalName());

  private final ServiceGroup services = new DefaultServiceGroup();
  private final int port;

  private Channel channel;
  private EventLoopGroup parentGroup;
  private EventLoopGroup childGroup;

  public HttpJsonRpcServer(int port) {
    this.port = port;
  }

  @Override
  public ServiceGroup serviceGroup() {
    return services;
  }

  /**
   * Starts this server.
   *
   * <p>This is a synchronous operation.</p>
   */
  public void start() {
    ServerBootstrap bootstrap = new ServerBootstrap();
    parentGroup = new NioEventLoopGroup();
    childGroup = new NioEventLoopGroup();

    bootstrap.group(parentGroup, childGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<Channel>() {

          @Override
          protected void initChannel(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("encode", new HttpRequestDecoder());
            pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
            pipeline.addLast("json-rpc", new JsonRpcHandler());
          }
        });

    ChannelFuture futureChannel = bootstrap.bind(port).awaitUninterruptibly();
    if (futureChannel.isSuccess()) {
      this.channel = futureChannel.channel();
      logger.info("Piezo JSON-RPC server started successfully.");
    } else {
      logger.info("Failed to start Piezo JSON-RPC server.");
      throw new RuntimeException(futureChannel.cause());
    }
  }

  /**
   * Stops this server.
   *
   * <p>This is a synchronous operation.</p>
   */
  public void stop() {
    logger.info("Shutting down Piezo server.");
    channel.close().addListener(new GenericFutureListener<Future<Void>>() {

      @Override
      public void operationComplete(Future<Void> future) throws Exception {
        parentGroup.shutdownGracefully();
        childGroup.shutdownGracefully();
      }
    }).awaitUninterruptibly();
  }

  private final class JsonRpcHandler extends SimpleChannelInboundHandler<HttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
      if (!request.getMethod().equals(HttpMethod.POST)) {
        // return error
      }

      
    }
  }
}
