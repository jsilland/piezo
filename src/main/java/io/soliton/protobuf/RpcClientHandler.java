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

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Client handler in charge of decoding the server's response and dispatching
 * it to the relevant client.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Envelope> {

  private static final Logger logger = Logger.getLogger(RpcClientHandler.class.getCanonicalName());
  private static final Random RANDOM = new Random();

  private final ConcurrentMap<Long, ResponseFuture<? extends Message>> inFlightRequests =
      new MapMaker().makeMap();
  private Channel channel;

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelRead0(ChannelHandlerContext context, Envelope response) throws Exception {
    ResponseFuture<? extends Message> future = inFlightRequests.remove(response.getRequestId());
    if (future == null) {
      logger.warning(String.format("Received response from %s for unknown request id: %d",
          channel.remoteAddress(), response.getRequestId()));
      return;
    }
    future.setResponse(response);
  }

  public <O extends Message> ResponseFuture<O> newProvisionalResponse(Parser<O> outputParser) {
    long requestId = RANDOM.nextLong();
    ResponseFuture<O> outputFuture = new ResponseFuture<>(requestId, new Cancel(requestId),
        outputParser);
    inFlightRequests.put(requestId, outputFuture);
    return outputFuture;
  }

  public ListenableFuture<? extends Message> finish(long requestId) {
    return inFlightRequests.remove(requestId);
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
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

    @Override
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
