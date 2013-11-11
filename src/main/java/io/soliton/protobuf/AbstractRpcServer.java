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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.logging.Logger;

/**
 * Provides a default implementation of {@link Server} which binds to a TCP
 * port.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public abstract class AbstractRpcServer implements Server {

  private static final Logger logger = Logger.getLogger(AbstractRpcServer.class.getCanonicalName());

  private final int port;
  private final ServiceGroup serviceGroup = new DefaultServiceGroup();
  private final Class<? extends ServerChannel> channelClass;
  private final EventLoopGroup parentGroup;
  private final EventLoopGroup childGroup;

  private Channel channel;

  protected AbstractRpcServer(int port, Class<? extends ServerChannel> channelClass,
      EventLoopGroup parentGroup, EventLoopGroup childGroup) {
    this.port = port;
    this.channelClass = channelClass;
    this.parentGroup = parentGroup;
    this.childGroup = childGroup;
  }

  /**
   * Returns the set of services surfaced on this server.
   */
  public ServiceGroup serviceGroup() {
    return serviceGroup;
  }

  /**
   * Starts this server.
   * <p/>
   * <p>This is a synchronous operation.</p>
   */
  public void start() throws Exception {
    logger.info(String.format("Starting RPC server on port %d", port));
    ServerBootstrap bootstrap = new ServerBootstrap();

    ChannelFuture futureChannel = bootstrap.group(parentGroup, childGroup)
        .channel(channelClass)
        .childHandler(channelInitializer())
        .bind(port)
        .awaitUninterruptibly();

    if (futureChannel.isSuccess()) {
      this.channel = futureChannel.channel();
      logger.info("RPC server started successfully.");
    } else {
      logger.info("Failed to start RPC server.");
      throw new Exception(futureChannel.cause());
    }
  }

  /**
   * Stops this server.
   * <p/>
   * <p>This is a synchronous operation.</p>
   */
  public void stop() {
    logger.info("Shutting down RPC server.");
    channel.close().addListener(new GenericFutureListener<Future<Void>>() {

      @Override
      public void operationComplete(Future<Void> future) throws Exception {
        parentGroup.shutdownGracefully();
        childGroup.shutdownGracefully();
      }
    }).awaitUninterruptibly();
  }

  /**
   * Implemented by subclasses to customize their handling of incoming
   * requests.
   *
   * @see ChannelInitializers
   */
  protected abstract ChannelInitializer<? extends Channel> channelInitializer();
}
