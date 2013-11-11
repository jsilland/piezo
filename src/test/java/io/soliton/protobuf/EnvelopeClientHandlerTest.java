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

import io.soliton.protobuf.testing.TimeResponse;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.Parser;
import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link io.soliton.protobuf.EnvelopeClientHandler}
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class EnvelopeClientHandlerTest {

  private static class IdentityEnvelopeClientHandler extends EnvelopeClientHandler<Envelope,
      Envelope> {

    @Override
    public Envelope convertRequest(Envelope request) {
      return request;
    }

    @Override
    public Envelope convertResponse(Envelope response) throws ResponseConversionException {
      return response;
    }
  }

  private static final ClientMethod<TimeResponse> CLIENT_METHOD = new ClientMethod<TimeResponse>() {
    @Override
    public String serviceName() {
      return null;
    }

    @Override
    public String name() {
      return null;
    }

    @Override
    public Parser<TimeResponse> outputParser() {
      return TimeResponse.PARSER;
    }

    @Override
    public Builder outputBuilder() {
      return null;
    }
  };

  @Test
  public void testProvisionAndCancel() {
    EnvelopeClientHandler handler = new IdentityEnvelopeClientHandler();

    Channel channel = Mockito.mock(Channel.class);
    handler.setChannel(channel);
    handler.setClientLogger(new NullClientLogger());
    EnvelopeFuture<TimeResponse> future = handler.newProvisionalResponse(CLIENT_METHOD);
    future.cancel(true);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(channel).writeAndFlush(captor.capture());
    Object argument = captor.getValue();
    Assert.assertTrue(argument instanceof Envelope);
    Envelope cancellation = (Envelope) argument;
    Assert.assertEquals(future.requestId(), cancellation.getRequestId());
    Assert.assertTrue(cancellation.hasControl());
    Assert.assertTrue(cancellation.getControl().getCancel());
  }

  @Test
  public void testReceiveResponse() throws Exception {
    EnvelopeClientHandler handler = new IdentityEnvelopeClientHandler();
    handler.setClientLogger(new NullClientLogger());
    EnvelopeFuture<TimeResponse> future = handler.newProvisionalResponse(CLIENT_METHOD);

    final CountDownLatch latch = new CountDownLatch(1);
    FutureCallback<TimeResponse> callback = new FutureCallback<TimeResponse>() {
      @Override
      public void onSuccess(@Nullable TimeResponse result) {
        Assert.assertEquals(5L, result.getTime());
        latch.countDown();
      }

      @Override
      public void onFailure(Throwable t) {
      }
    };

    Futures.addCallback(future, callback);

    Envelope response = Envelope.newBuilder()
        .setRequestId(future.requestId())
        .setPayload(TimeResponse.newBuilder().setTime(5L).build().toByteString())
        .build();
    handler.channelRead0(null, response);

    latch.await(5, TimeUnit.SECONDS);
  }
}
