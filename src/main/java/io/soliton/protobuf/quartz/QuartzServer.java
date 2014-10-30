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
import io.soliton.protobuf.NullServerLogger;
import io.soliton.protobuf.ServerLogger;

import javax.net.ssl.SSLContext;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * A server implementation surfacing services over an HTTP transport and
 * encoding RPC responses as {@link io.soliton.protobuf.Envelope}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public abstract class QuartzServer extends AbstractRpcServer {

	/**
	 * Returns a new builder to configure instances of Quartz server.
	 *
	 * @param port the port this server should bind to
	 */
	public static Builder newServer(int port) {
		return new Builder(port);
	}

	/**
	 * Creates a new server configure to bind to the given TCP port.
	 *
	 * @param port the TCP port to bind to
	 */
	public QuartzServer(int port) {
		super(port, NioServerSocketChannel.class, new NioEventLoopGroup(),
				new NioEventLoopGroup());
	}

	/**
	 * Configurable builder of {@link QuartzServer} instances.
	 */
	public static final class Builder {

		private final int port;
		private String path = QuartzProtocol.DEFAULT_PATH;
		private SSLContext sslContext;
		private ServerLogger serverLogger = new NullServerLogger();

		private Builder(int port) {
			Preconditions.checkArgument(port > 0 && port < 65536);
			this.port = port;
		}

		/**
		 * Sets the encryption context this server should respect when new channels
		 * are opened.
		 *
		 * @param sslContext a configured context for SSL operations
		 * @return {@code this} instance
		 */
		public Builder setSslContext(SSLContext sslContext) {
			this.sslContext = Preconditions.checkNotNull(sslContext);
			return this;
		}

		/**
		 * Sets the URI path prefix that should be validated when receiving
		 * requests.
		 *
		 * @return {@code this} instance
		 */
		public Builder setPath(String path) {
			Preconditions.checkNotNull(path);
			Preconditions.checkArgument(path.startsWith("/"));
			this.path = path;
			return this;
		}

		/**
		 * Configures the object this server should log operations to.
		 *
		 * @param serverLogger the server-side monitoring logger
		 * @return {@code this} instance
		 */
		public Builder setServerLogger(ServerLogger serverLogger) {
			this.serverLogger = serverLogger;
			return this;
		}

		/**
		 * Instantiates and returns a new server which has bound to the configured
		 * TPC port.
		 */
		public QuartzServer build() {
			return new QuartzServer(port) {
				protected ChannelInitializer<? extends Channel> channelInitializer() {
					return sslContext == null ?
						   ChannelInitializers
								   .httpServer(new QuartzServerHandler(serviceGroup(), path,
										   serverLogger)) :
						   ChannelInitializers
								   .secureHttpServer(new QuartzServerHandler(serviceGroup(), path,
										   serverLogger), sslContext);
				}
			};
		}
	}
}
