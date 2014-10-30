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

import io.soliton.protobuf.testing.TimeRequest;
import io.soliton.protobuf.testing.TimeResponse;
import io.soliton.protobuf.testing.TimeService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Tests for {@link io.soliton.protobuf.EnvelopeServerHandler}
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class EnvelopeServerHandlerTest {

	private static class IdentityServerHandler extends EnvelopeServerHandler<Envelope, Envelope> {

		public IdentityServerHandler(ServiceGroup services, ServerLogger serverLogger) {
			super(services, serverLogger);
		}

		@Override
		protected Envelope convertRequest(Envelope request) {
			return request;
		}

		@Override
		protected Envelope convertResponse(Envelope response) {
			return response;
		}
	}

	@Test
	public void testNormalExecution() throws Exception {
		Envelope request = Envelope.newBuilder()
				.setRequestId(1L)
				.setService("soliton.piezo.testing.TimeService")
				.setMethod("GetTime")
				.setPayload(TimeRequest.newBuilder().setTimezone("UTC").build().toByteString())
				.build();

		Channel channel = Mockito.mock(Channel.class);
		ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
		Mockito.when(context.channel()).thenReturn(channel);

		final CountDownLatch latch = new CountDownLatch(1);
		final ServerMethod<TimeRequest, TimeResponse> serverMethod =
				new ServerMethod<TimeRequest, TimeResponse>() {
					@Override
					public String name() {
						return null;
					}

					@Override
					public Parser<TimeRequest> inputParser() {
						return TimeRequest.PARSER;
					}

					@Override
					public Message.Builder inputBuilder() {
						return null;
					}

					@Override
					public ListenableFuture<TimeResponse> invoke(TimeRequest request) {
						Assert.assertEquals("UTC", request.getTimezone());
						latch.countDown();
						return SettableFuture.create();
					}
				};

		Answer<ServerMethod<TimeRequest, TimeResponse>> answer = new Answer<ServerMethod
				<TimeRequest, TimeResponse>>() {

			@Override
			public ServerMethod<TimeRequest, TimeResponse> answer(
					InvocationOnMock invocation) throws Throwable {
				return serverMethod;
			}
		};

		Service service = Mockito.mock(Service.class);
		Mockito.when(service.fullName()).thenReturn("soliton.piezo.testing.TimeService");
		Mockito.when(service.lookup(Mockito.anyString())).thenAnswer(answer);
		ServiceGroup services = new DefaultServiceGroup();
		services.addService(service);
		EnvelopeServerHandler handler = new IdentityServerHandler(services,
				new NullServerLogger());

		handler.channelRead0(context, request);

		latch.await(5, TimeUnit.SECONDS);
		Assert.assertEquals(1, handler.pendingRequests().size());
	}

	@Test
	public void testCancelRequest() throws Exception {
		Envelope request = Envelope.newBuilder()
				.setRequestId(1L)
				.setService("soliton.piezo.testing.TimeService")
				.setMethod("GetTime")
				.setPayload(TimeRequest.newBuilder().setTimezone("UTC").build().toByteString())
				.build();

		Channel channel = Mockito.mock(Channel.class);
		ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
		Mockito.when(context.channel()).thenReturn(channel);

		final ListenableFuture<TimeResponse> future = Mockito.mock(ListenableFuture.class);
		Mockito.when(future.cancel(Mockito.anyBoolean())).thenReturn(true);

		final ServerMethod<TimeRequest, TimeResponse> serverMethod =
				new ServerMethod<TimeRequest, TimeResponse>() {
					@Override
					public String name() {
						return null;
					}

					@Override
					public Parser<TimeRequest> inputParser() {
						return TimeRequest.PARSER;
					}

					@Override
					public Message.Builder inputBuilder() {
						return null;
					}

					@Override
					public ListenableFuture<TimeResponse> invoke(TimeRequest request) {
						return future;
					}
				};

		Answer<ServerMethod<TimeRequest, TimeResponse>> answer = new Answer<ServerMethod
				<TimeRequest, TimeResponse>>() {

			@Override
			public ServerMethod<TimeRequest, TimeResponse> answer(
					InvocationOnMock invocation) throws Throwable {
				return serverMethod;
			}
		};

		Service service = Mockito.mock(Service.class);
		Mockito.when(service.fullName()).thenReturn("soliton.piezo.testing.TimeService");
		Mockito.when(service.lookup(Mockito.anyString())).thenAnswer(answer);
		ServiceGroup services = new DefaultServiceGroup();
		services.addService(service);
		EnvelopeServerHandler handler = new IdentityServerHandler(services,
				new NullServerLogger());

		handler.channelRead0(context, request);

		Envelope cancel = Envelope.newBuilder()
				.setRequestId(1L)
				.setControl(Control.newBuilder().setCancel(true))
				.build();

		handler.channelRead0(context, cancel);
		Mockito.verify(future).cancel(true);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		Mockito.verify(channel).writeAndFlush(captor.capture());
		Object captured = captor.getValue();
		Assert.assertTrue(captured instanceof Envelope);
		Envelope response = (Envelope) captured;
		Assert.assertEquals(1L, response.getRequestId());
		Assert.assertTrue(response.getControl().getCancel());
	}

	@Test
	public void testUnknownService() throws Exception {
		Envelope request = Envelope.newBuilder()
				.setRequestId(1L)
				.setService("Unknown")
				.build();
		Channel channel = Mockito.mock(Channel.class);
		ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
		Mockito.when(context.channel()).thenReturn(channel);

		EnvelopeServerHandler handler = new IdentityServerHandler(
				new DefaultServiceGroup(), new NullServerLogger());
		handler.channelRead0(context, request);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		Mockito.verify(channel).writeAndFlush(captor.capture());

		Object captured = captor.getValue();
		Assert.assertTrue(captured instanceof Envelope);
		Envelope response = (Envelope) captured;
		Assert.assertTrue(response.hasControl());
		Assert.assertTrue(response.getControl().hasError());
		Assert.assertTrue(response.getControl().getError().contains("service"));
	}

	@Test
	public void testUnkownMethod() throws Exception {
		Service timeService = TimeService.newService(new TimeServer());
		Envelope request = Envelope.newBuilder()
				.setRequestId(1L)
				.setService(timeService.fullName())
				.setMethod("Unknown")
				.build();
		Channel channel = Mockito.mock(Channel.class);
		ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
		Mockito.when(context.channel()).thenReturn(channel);

		ServiceGroup services = new DefaultServiceGroup();
		services.addService(timeService);

		EnvelopeServerHandler handler = new IdentityServerHandler(services,
				new NullServerLogger());
		handler.channelRead0(context, request);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		Mockito.verify(channel).writeAndFlush(captor.capture());

		Object captured = captor.getValue();
		Assert.assertTrue(captured instanceof Envelope);
		Envelope response = (Envelope) captured;
		Assert.assertTrue(response.hasControl());
		Assert.assertTrue(response.getControl().hasError());
		Assert.assertTrue(response.getControl().getError().contains("method"));
	}
}
