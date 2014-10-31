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

import io.soliton.protobuf.ClientLogger;
import io.soliton.protobuf.ClientMethod;
import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.testing.TimeRequest;
import io.soliton.protobuf.testing.TimeResponse;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests for {@link RpcClient}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcClientTest {

	@Test
	public void testEncodeMethodCallSuccess() throws InvalidProtocolBufferException {
		Channel channel = Mockito.mock(Channel.class);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		ChannelFuture success = Mockito.mock(ChannelFuture.class);
		Mockito.when(success.isDone()).thenReturn(true);
		Mockito.when(success.isSuccess()).thenReturn(true);
		Mockito.when(channel.writeAndFlush(captor.capture())).thenReturn(success);
		RpcClientHandler handler = new RpcClientHandler();
		RpcClient client = new RpcClient(channel, handler, ClientLogger.NULL_LOGGER);

		ClientMethod<TimeResponse> method = Mockito.mock(ClientMethod.class);
		Mockito.when(method.serviceName()).thenReturn("TimeService");
		Mockito.when(method.name()).thenReturn("GetTime");
		Mockito.when(method.outputParser()).thenReturn(TimeResponse.PARSER);

		client.encodeMethodCall(method, TimeRequest.newBuilder().setTimezone("UTC").build());

		Assert.assertEquals(1, handler.inFlightRequests().size());

		Object captured = captor.getValue();
		Assert.assertTrue(captured instanceof Envelope);
		Envelope request = (Envelope) captured;
		Assert.assertTrue(request.hasPayload());
		TimeRequest timeRequest = TimeRequest.parseFrom(request.getPayload());
		Assert.assertEquals("UTC", timeRequest.getTimezone());
	}

	@Test
	public void testEncodeMethodCallFailure() throws InvalidProtocolBufferException,
			InterruptedException {
		Channel channel = Mockito.mock(Channel.class);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

		DefaultChannelPromise failure = new DefaultChannelPromise(
				channel, ImmediateEventExecutor.INSTANCE);
		failure.setFailure(new Exception("OMGWTF"));
		Mockito.when(channel.writeAndFlush(captor.capture())).thenReturn(failure);

		RpcClientHandler handler = new RpcClientHandler();
		RpcClient client = new RpcClient(channel, handler, ClientLogger.NULL_LOGGER);

		ClientMethod<TimeResponse> method = Mockito.mock(ClientMethod.class);
		Mockito.when(method.serviceName()).thenReturn("TimeService");
		Mockito.when(method.name()).thenReturn("GetTime");
		Mockito.when(method.outputParser()).thenReturn(TimeResponse.PARSER);

		final CountDownLatch latch = new CountDownLatch(1);
		FutureCallback<TimeResponse> callback = new FutureCallback<TimeResponse>() {
			@Override
			public void onSuccess(@Nullable TimeResponse result) {
			}

			@Override
			public void onFailure(Throwable t) {
				Assert.assertEquals("OMGWTF", t.getMessage());
				latch.countDown();
			}
		};

		ListenableFuture<TimeResponse> future = client.encodeMethodCall(
				method, TimeRequest.newBuilder().setTimezone("UTC").build());

		Futures.addCallback(future, callback);
		latch.await(5, TimeUnit.SECONDS);

		Assert.assertEquals(0, handler.inFlightRequests().size());
	}
}
