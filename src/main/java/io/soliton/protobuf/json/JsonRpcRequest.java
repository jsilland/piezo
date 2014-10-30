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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Structured representation of a JSON-RPC request.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcRequest {

	private static final Joiner DOT_JOINER = Joiner.on('.');
	private static final Splitter DOT_SPLITTER = Splitter.on('.').trimResults().omitEmptyStrings();


	private final String service;
	private final String method;
	private final JsonElement id;
	private final JsonObject parameter;

	/**
	 * Exhaustive constructor
	 *
	 * @param service the service this call is targeting
	 * @param method the method this call is targeting
	 * @param id the generic identifier of the request, as set by the client
	 * @param parameter the sole parameter of this call
	 */
	public JsonRpcRequest(String service, String method, JsonElement id,
			JsonObject parameter) {
		this.service = service;
		this.method = method;
		this.id = id;
		this.parameter = parameter;
	}

	public String service() {
		return service;
	}

	public String method() {
		return method;
	}

	public JsonElement id() {
		return id;
	}

	public JsonObject parameter() {
		return parameter;
	}

	public JsonObject toJson() {
		JsonObject request = new JsonObject();
		request.add(JsonRpcProtocol.ID, id());
		request.add(JsonRpcProtocol.METHOD,
				new JsonPrimitive(DOT_JOINER.join(service(), method())));
		JsonArray params = new JsonArray();
		params.add(parameter());
		request.add(JsonRpcProtocol.PARAMETERS, params);
		return request;
	}

	public static JsonRpcRequest fromJson(JsonElement root) throws JsonRpcError {
		if (!root.isJsonObject()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Received payload is not a JSON Object");
		}

		JsonObject request = root.getAsJsonObject();
		JsonElement id = request.get(JsonRpcProtocol.ID);
		JsonElement methodNameElement = request.get(JsonRpcProtocol.METHOD);
		JsonElement paramsElement = request.get(JsonRpcProtocol.PARAMETERS);

		if (id == null) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Malformed request, missing 'id' property");
		}

		if (methodNameElement == null) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Malformed request, missing 'method' property");
		}

		if (paramsElement == null) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Malformed request, missing 'params' property");
		}


		if (!methodNameElement.isJsonPrimitive()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Method name is not a JSON primitive");
		}

		JsonPrimitive methodName = methodNameElement.getAsJsonPrimitive();
		if (!methodName.isString()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Method name is not a string");
		}

		if (!paramsElement.isJsonArray()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'params' property is not an array");
		}

		JsonArray params = paramsElement.getAsJsonArray();
		if (params.size() != 1) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'params' property is not an array");
		}

		JsonElement paramElement = params.get(0);
		if (!paramElement.isJsonObject()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"Parameter is not an object");
		}

		JsonObject parameter = paramElement.getAsJsonObject();
		List<String> serviceAndMethod = Lists.newArrayList(DOT_SPLITTER.split(
				methodName.getAsString()));


		String methodNameString = methodName.getAsString();
		int dotIndex = methodNameString.lastIndexOf('.');

		if (dotIndex < 0) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'method' property is not properly formatted");
		}

		if (dotIndex == methodNameString.length() - 1) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'method' property is not properly formatted");
		}

		String service = methodNameString.substring(0, dotIndex);
		String method = methodNameString.substring(dotIndex + 1);

		if (service.isEmpty() || method.isEmpty()) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'method' property is not properly formatted");
		}

		if (serviceAndMethod.size() < 2) {
			throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
					"'method' property is not properly formatted");
		}

		return new JsonRpcRequest(service, method, id, parameter);
	}
}
