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
import com.google.common.collect.MapMaker;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * A simple transport implementation connecting to a remote server over a
 * TCP link.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TcpClient extends ChannelInboundMessageHandlerAdapter<Envelope>
		implements Client {

	private static final Logger logger = Logger.getLogger(
			TcpClient.class.getCanonicalName());
	private static final Random RANDOM = new Random();
	
	private final ConcurrentMap<Long, ResponseFuture<? extends Message>> inFlightRequests =
			new MapMaker().makeMap();
	
	private final HostAndPort remoteAddress;
	private final Channel channel;
	
	/**
	 * Creates a new, single transport connected to the given remote host.
	 * 
	 * @param remoteAddress the coordinates of the remote host to connect to
	 */
	public TcpClient(HostAndPort remoteAddress) {
		this.remoteAddress = Preconditions.checkNotNull(remoteAddress);
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    bootstrap.channel(NioSocketChannel.class);
		bootstrap.handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline().addLast("frameDecoder",
						new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
				channel.pipeline().addLast("protobufDecoder",
            new ProtobufDecoder(Envelope.getDefaultInstance()));
				channel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
				channel.pipeline().addLast("protobufEncoder", new ProtobufEncoder());
				channel.pipeline().addLast("piezoSimpleTransport", TcpClient.this);
			}
		});
		
		ChannelFuture future = bootstrap.connect(remoteAddress.getHostText(), remoteAddress.getPort());
		future.awaitUninterruptibly();
		if (future.isSuccess()) {
      logger.info("Piezo client successfully connected to " + remoteAddress.toString());
			this.channel = future.channel();
		} else {
      logger.warning("Piezo client failed to connect to " + remoteAddress.toString());
			throw new RuntimeException(future.cause());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public <O extends Message> ListenableFuture<O> encodeMethodCall(
			ClientMethod<O> method, Message input) {
		final long requestId = RANDOM.nextLong();
		final ResponseFuture<O> output = new ResponseFuture<O>(
				new Cancel(requestId), method.outputParser());
		Envelope request = Envelope.newBuilder()
				.setRequestId(requestId)
				.setService(method.serviceName())
				.setMethod(method.name())
				.setPayload(input.toByteString())
				.build();
		channel.write(request).addListener(new GenericFutureListener<ChannelFuture>() {

			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					inFlightRequests.put(requestId, output);
				} else {
					output.setException(future.cause());
				}
			}
			
		});
		return output;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageReceived(ChannelHandlerContext context, Envelope response) throws Exception {
		ResponseFuture<? extends Message> future = inFlightRequests.remove(response.getRequestId());
		if (future == null) {
			logger.warning(String.format("Received response from %s for unknown request id: %d",
					remoteAddress.toString(), response.getRequestId()));
			return;
		}
		future.set(response);
	}
	
	/**
	 * Implements the cancellation logic for an individual RPC.
	 *
	 * @author Julien Silland (julien@soliton.io)
	 */
	class Cancel implements Runnable {
		private final long requestId;
		
		public Cancel(long requestId) {
			this.requestId = requestId;
		}
		
		public void run() {
			if (inFlightRequests.remove(requestId) != null) {
				Envelope request = Envelope.newBuilder()
						.setRequestId(requestId)
						.setControl(Control.newBuilder().setCancel(true))
						.build();
				channel.write(request);
			}
		}
	}

}
