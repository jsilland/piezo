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

import io.soliton.protobuf.ServerLogger;
import io.soliton.protobuf.ServerMethod;
import io.soliton.protobuf.Service;
import io.soliton.protobuf.ServiceGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcRequestInvoker {

	private static final ExecutorService TRANSFORM_EXECUTOR = Executors.newCachedThreadPool();

	private final ServiceGroup services;
	private final ServerLogger serverLogger;

	public JsonRpcRequestInvoker(ServiceGroup services, ServerLogger serverLogger) {
		this.services = services;
		this.serverLogger = serverLogger;
	}

	/**
	 * Executes a request.
	 *
	 * @param request the request to invoke
	 * @return a handle on the future result of the invocation
	 */
	public ListenableFuture<JsonRpcResponse> invoke(JsonRpcRequest request) {
		Service service = services.lookupByName(request.service());
		if (service == null) {
			JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Unknown service: " + request.service());
			JsonRpcResponse response = JsonRpcResponse.error(error);
			return Futures.immediateFuture(response);
		}

		ServerMethod<? extends Message, ? extends Message> method = service
				.lookup(request.method());

		serverLogger.logMethodCall(service, method);

		if (method == null) {
			JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Unknown method: " + request.service());
			JsonRpcResponse response = JsonRpcResponse.error(error);
			return Futures.immediateFuture(response);
		}

		return invoke(method, request.parameter(), request.id());
	}

	/**
	 * Actually invokes the server method.
	 *
	 * @param method the method to invoke
	 * @param parameter the request's parameter
	 * @param id the request's client-side identifier
	 * @param <I> the method's input proto-type
	 * @param <O> the method's output proto-type
	 */
	private <I extends Message, O extends Message> ListenableFuture<JsonRpcResponse> invoke(
			ServerMethod<I, O> method, JsonObject parameter, JsonElement id) {
		I request;
		try {
			request = (I) Messages.fromJson(method.inputBuilder(), parameter);
		} catch (Exception e) {
			serverLogger.logServerFailure(method, e);
			SettableFuture<JsonRpcResponse> future = SettableFuture.create();
			future.setException(e);
			return future;
		}
		ListenableFuture<O> response = method.invoke(request);
		return Futures.transform(response, new JsonConverter(id), TRANSFORM_EXECUTOR);
	}

	private class JsonConverter implements Function<Message, JsonRpcResponse> {

		private final JsonElement id;

		private JsonConverter(JsonElement id) {
			this.id = id;
		}

		@Override
		public JsonRpcResponse apply(Message output) {
			return JsonRpcResponse.success(Messages.toJson(output), id);
		}
	}
}
