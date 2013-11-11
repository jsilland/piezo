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

package io.soliton.protobuf.quartz;

import io.soliton.protobuf.AbstractRpcServer;
import io.soliton.protobuf.ChannelInitializers;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.net.ssl.SSLContext;

/**
 * A server implementation surfacing services over an HTTP transport and
 * encoding RPC responses as {@link io.soliton.protobuf.Envelope}.
 */
public class QuartzServer extends AbstractRpcServer {

  public static Builder newServer(int port) {
    return new Builder(port);
  }

  /**
   * Creates a new server configure to bind to the given TCP port.
   *
   * @param port the TCP port to bind to
   */
  public QuartzServer(int port) {
    super(port, NioServerSocketChannel.class, new NioEventLoopGroup(), new NioEventLoopGroup());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ChannelInitializer<? extends Channel> channelInitializer() {
    return ChannelInitializers.httpServer(new QuartzServerHandler(serviceGroup()));
  }

  public static final class Builder {

    private final int port;
    private String path = "/quartz";
    private SSLContext sslContext;

    private Builder(int port) {
      Preconditions.checkArgument(port > 0 && port < 65536);
      this.port = port;
    }

    public Builder setSslContext(SSLContext sslContext) {
      this.sslContext = Preconditions.checkNotNull(sslContext);
      return this;
    }

    public Builder setPath(String path) {
      this.path = Preconditions.checkNotNull(path);
      return this;
    }

    public QuartzServer build() {
      return new QuartzServer(port) {
        protected ChannelInitializer<? extends Channel> channelInitializer() {
          return sslContext == null ?
              ChannelInitializers.httpServer(new QuartzServerHandler(serviceGroup())) :
              ChannelInitializers.secureHttpServer(new QuartzServerHandler(serviceGroup()),
                  sslContext);
        }
      };
    }
  }
}
