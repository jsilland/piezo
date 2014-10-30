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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.ssl.SslHandler;

/**
 * Utility methods pertaining to {@link ChannelInitializer}s.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class ChannelInitializers {

	/**
	 * Returns a new channel initializer suited to encode and decode a protocol
	 * buffer message.
	 * <p/>
	 * <p>Message sizes over 10 MB are not supported.</p>
	 * <p/>
	 * <p>The handler will be executed on the I/O thread. Blocking operations
	 * should be executed in their own thread.</p>
	 *
	 * @param defaultInstance an instance of the message to handle
	 * @param handler the handler implementing the application logic
	 * @param <M> the type of the support protocol buffer message
	 */
	public static final <M extends Message> ChannelInitializer<Channel> protoBuf(
			final M defaultInstance, final SimpleChannelInboundHandler<M> handler) {
		return new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline().addLast("frameDecoder",
						new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
				channel.pipeline().addLast("protobufDecoder",
						new ProtobufDecoder(defaultInstance));
				channel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
				channel.pipeline().addLast("protobufEncoder", new ProtobufEncoder());
				channel.pipeline().addLast("applicationHandler", handler);
			}
		};
	}

	/**
	 * Returns a new chanel initializer suited to decode and process HTTP
	 * requests.
	 *
	 * @param handler the handler implementing the application logic
	 */
	public static final ChannelInitializer<Channel> httpServer(
			final SimpleChannelInboundHandler<HttpRequest> handler) {
		Preconditions.checkArgument(handler.isSharable());
		return new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				pipeline.addLast("httpCodec", new HttpServerCodec());
				pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
				pipeline.addLast("httpServerHandler", handler);
			}
		};
	}

	/**
	 * Returns a server-side channel initializer capable of securely receiving
	 * and sending HTTP requests and responses
	 * <p/>
	 * <p>Communications will be encrypted as per the configured SSL context</p>
	 *
	 * @param handler the handler implementing the business logic.
	 * @param sslContext the SSL context which drives the security of the
	 * link to the client.
	 */
	public static final ChannelInitializer<Channel> secureHttpServer(
			final SimpleChannelInboundHandler<HttpRequest> handler,
			final SSLContext sslContext) {
		return new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(false);
				pipeline.addLast("ssl", new SslHandler(sslEngine));
				pipeline.addLast("httpCodec", new HttpServerCodec());
				pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
				pipeline.addLast("httpServerHandler", handler);
			}
		};
	}

	/**
	 * Returns a channel initializer suited to decode and process HTTP responses.
	 *
	 * @param handler the handler implementing the application logic
	 */
	public static final ChannelInitializer<Channel> httpClient(
			final SimpleChannelInboundHandler<HttpResponse> handler) {
		return new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				pipeline.addLast("httpCodec", new HttpClientCodec());
				pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
				pipeline.addLast("httpClientHandler", handler);
			}
		};
	}

	/**
	 * Returns a client-side channel initializer capable of securely sending
	 * and receiving HTTP requests and responses.
	 * <p/>
	 * <p>Communications will be encrypted as per the configured SSL context</p>
	 *
	 * @param handler the handler in charge of implementing the business logic
	 * @param sslContext the SSL context which drives the security of the
	 * link to the server.
	 */
	public static final ChannelInitializer<Channel> secureHttpClient(
			final SimpleChannelInboundHandler<HttpResponse> handler,
			final SSLContext sslContext) {
		return new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(true);
				pipeline.addLast("ssl", new SslHandler(sslEngine));
				pipeline.addLast("httpCodec", new HttpClientCodec());
				pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
				pipeline.addLast("httpClientHandler", handler);
			}
		};
	}
}
