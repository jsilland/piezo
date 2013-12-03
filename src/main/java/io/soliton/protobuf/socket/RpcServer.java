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

package io.soliton.protobuf.socket;

import io.soliton.protobuf.AbstractRpcServer;
import io.soliton.protobuf.ChannelInitializers;
import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.NullServerLogger;
import io.soliton.protobuf.ServerLogger;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Simple implementation of {@link io.soliton.protobuf.Server} using a TCP
 * transport.
 *
 * <p>This server uses a proprietary binary protocol to communicate with
 * instances of {@link RpcClient}.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcServer extends AbstractRpcServer {

  /**
   * Returns a new builder for configuring instances of this class.
   *
   * @param port the port the server should bind to
   */
  public static Builder newServer(int port) {
    return new Builder(port);
  }

  /**
   * Creates a new server configure to bind to the given TCP port.
   *
   * @param port the TCP port to bind to
   */
  private RpcServer(int port) {
    super(port, NioServerSocketChannel.class, new NioEventLoopGroup(), new NioEventLoopGroup());
  }

  protected ChannelInitializer<? extends Channel> channelInitializer() {
    return ChannelInitializers.protoBuf(Envelope.getDefaultInstance(),
        new RpcServerHandler(serviceGroup(), new NullServerLogger()));
  }

  /**
   * A configurable builder for obtaining {@link RpcServer} instances
   *
   * @author Julien Silland (julien@soliton.io)
   */
  public static class Builder {

    private final int port;
    private ServerLogger serverLogger = new NullServerLogger();

    private Builder(int port) {
      Preconditions.checkArgument(port > 0 && port < 65536);
      this.port = port;
    }

    /**
     * Sets the monitoring logger to log server operations to.
     *
     * @param serverLogger the server-side logger
     * @return {@code this} instance
     */
    public Builder setServerLogger(ServerLogger serverLogger) {
      this.serverLogger = Preconditions.checkNotNull(serverLogger);
      return this;
    }

    /**
     * Construct a new {@link RpcServer}, as per this builder's configuration
     */
    public RpcServer build() {
      return new RpcServer(port) {
        protected ChannelInitializer<? extends Channel> channelInitializer() {
          return ChannelInitializers.protoBuf(Envelope.getDefaultInstance(),
              new RpcServerHandler(serviceGroup(), serverLogger));
        }
      };
    }
  }
}
