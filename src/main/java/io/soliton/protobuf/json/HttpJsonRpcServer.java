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

import io.soliton.protobuf.AbstractRpcServer;
import io.soliton.protobuf.ChannelInitializers;
import io.soliton.protobuf.ServerLogger;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Concrete {@link io.soliton.protobuf.Server} implementation surfacing the
 * registered services over an HTTP transport using the JSON-RPC protocol.
 *
 * @author Julien Silland (julien@soliton.io)
 * @see <a href="http://json-rpc.org/">JSON-RPC</a>
 */
public abstract class HttpJsonRpcServer extends AbstractRpcServer {

	/**
	 * Creates a new builder for instances of {@link HttpJsonRpcServer}
	 *
	 * @param port the TCP port the server should bind to
	 */
	public static Builder newServer(int port) {
		return new Builder(port);
	}

	/**
	 * Exhaustive constructor.
	 *
	 * @param port the TCP port this server should bind to
	 */
	private HttpJsonRpcServer(int port) {
		super(port, NioServerSocketChannel.class, new NioEventLoopGroup(),
				new NioEventLoopGroup());
	}

	/**
	 * Configurable builder for instances of {@link HttpJsonRpcServer}
	 */
	public static class Builder {

		private final int port;
		private String rpcPath = JsonRpcProtocol.DEFAULT_RPC_PATH;
		private ServerLogger serverLogger = ServerLogger.NULL_LOGGER;

		private Builder(int port) {
			Preconditions.checkArgument(port > 0 && port < 65536);
			this.port = port;
		}

		/**
		 * Sets the URL path for which this server will be enabled.
		 *
		 * @return {@code this} object
		 */
		public Builder setRpcPath(String rpcPath) {
			;
			this.rpcPath = Preconditions.checkNotNull(rpcPath);
			Preconditions.checkArgument(rpcPath.startsWith("/"));
			return this;
		}

		/**
		 * Sets the logger the server will use.
		 *
		 * @return {@code this} object
		 */
		public Builder setServerLogger(ServerLogger serverLogger) {
			this.serverLogger = Preconditions.checkNotNull(serverLogger);
			return this;
		}

		/**
		 * Returns a new server as per the configuration of this builder.
		 * <p/>
		 * <p>This operation synchronously binds to the configured TCP port.</p>
		 */
		public HttpJsonRpcServer build() {
			return new HttpJsonRpcServer(port) {
				@Override
				protected ChannelInitializer<? extends Channel> channelInitializer() {
					return ChannelInitializers.httpServer(
							new JsonRpcServerHandler(this, rpcPath, serverLogger));
				}
			};
		}
	}
}
