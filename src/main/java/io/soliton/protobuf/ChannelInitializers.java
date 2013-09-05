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

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Utility methods pertaining to {@link ChannelInitializer}s.
 */
public class ChannelInitializers {

  /**
   * Returns a new channel initializer suited to encode a decode a protocol
   * buffer message.
   *
   * <p>Message sizes over 10 MB are not supported.</p>
   *
   * <p>The handler will be executed on the I/O thread. Consider using {@link #protoBuf(com.google.protobuf.Message, io.netty.channel.SimpleChannelInboundHandler, io.netty.util.concurrent.EventExecutorGroup)}
   * for a finer-grained control over the threading behavior.</p>
   *
   * @param defaultInstance an instance of the message to handle
   * @param handler the handler implementing the application logic
   * @param <M> the type of the support protocol buffer message
   * @see #protoBuf(com.google.protobuf.Message, io.netty.channel.SimpleChannelInboundHandler, io.netty.util.concurrent.EventExecutorGroup)
   */
  public static final <M extends Message> ChannelInitializer<Channel> protoBuf(
      final M defaultInstance, final SimpleChannelInboundHandler<M> handler) {
    return protoBuf(defaultInstance, handler, null);
  }

  /**
   * Returns a new channel initializer suited to encode a decode a protocol
   * buffer message.
   *
   * <p>Message sizes over 10 MB are not supported.</p>
   *
   * @param defaultInstance an instance of the message to handle
   * @param handler the handler implementing the application logic
   * @param executorGroup the executor to which handler work will be submitted
   * @param <M> the type of the support protocol buffer message
   * @see #protoBuf(com.google.protobuf.Message, io.netty.channel.SimpleChannelInboundHandler, io.netty.util.concurrent.EventExecutorGroup)
   */
  public static final <M extends Message> ChannelInitializer<Channel> protoBuf(
      final M defaultInstance, final SimpleChannelInboundHandler<M> handler,
      final EventExecutorGroup executorGroup) {
    return new ChannelInitializer<Channel>() {

      @Override
      protected void initChannel(Channel channel) throws Exception {
        channel.pipeline().addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
        channel.pipeline().addLast("protobufDecoder",
            new ProtobufDecoder(defaultInstance));
        channel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
        channel.pipeline().addLast("protobufEncoder", new ProtobufEncoder());
        channel.pipeline().addLast(executorGroup, "piezoServerTransport", handler);
      }
    };
  }

  public static final ChannelInitializer<Channel> httpServer(
      final SimpleChannelInboundHandler<HttpRequest> handler) {
    return httpServer(handler, null);
  }

  public static final ChannelInitializer<Channel> httpServer(
      final SimpleChannelInboundHandler<HttpRequest> handler,
      final EventExecutorGroup executorGroup) {
    Preconditions.checkArgument(handler.isSharable());
    return new ChannelInitializer<Channel>() {

      @Override
      protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        pipeline.addLast(executorGroup,"jsonRpcServer", handler);
      }
    };
  }

  public static final ChannelInitializer<Channel> httpClient(
      final SimpleChannelInboundHandler<HttpResponse> handler) {
    return httpClient(handler, null);
  }

  public static final ChannelInitializer<Channel> httpClient(
      final SimpleChannelInboundHandler<HttpResponse> handler,
      final EventExecutorGroup executorGroup) {
    return new ChannelInitializer<Channel>() {

      @Override
      protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("httpCodec", new HttpClientCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        pipeline.addLast(executorGroup, "jsonRpcClient", handler);
      }
    };
  }
}
