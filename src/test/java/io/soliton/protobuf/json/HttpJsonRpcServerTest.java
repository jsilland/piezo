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

import io.soliton.protobuf.Service;
import io.soliton.protobuf.TimeServer;
import io.soliton.protobuf.testing.TimeService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the JSON-RPC handler.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class HttpJsonRpcServerTest {

  private static int port;
  private static HttpJsonRpcServer server;

  private static int findAvailablePort() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();
    return port;
  }

  @BeforeClass
  public static void setUp() throws Exception {
    server = HttpJsonRpcServer.newServer(findAvailablePort()).build();
    Service timeService = TimeService.newService(new TimeServer());
    server.serviceGroup().addService(timeService);
    server.startUp();
  }

  @AfterClass
  public static void tearDown() {
    server.shutDown();
  }

  @Test
  public void testWrongPath() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", "TimeService.GetTime");
    request.addProperty("id", "identifier");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());
    JsonArray parameters = new JsonArray();
    parameters.add(parameter);
    request.add("params", parameters);

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/tox");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(404, response.error().status().code());
  }

  @Test
  public void testMissingBody() throws IOException {
    HttpContent httpContent = new ByteArrayContent("application/json",
        new byte[]{});

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/tox");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(400, response.error().status().code());
  }

  @Test
  public void testWrongHttpMethod() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", "TimeService.GetTime");
    request.addProperty("id", "identifier");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());
    JsonArray parameters = new JsonArray();
    parameters.add(parameter);
    request.add("params", parameters);

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/rpc");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPutRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(405, response.error().status().code());
  }

  @Test
  public void testMissingService() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", ".GetTime");
    request.addProperty("id", "identifier");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());
    JsonArray parameters = new JsonArray();
    parameters.add(parameter);
    request.add("params", parameters);

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/rpc");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(400, response.error().status().code());
  }

  @Test
  public void testMissingMethod() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", "TimeService.");
    request.addProperty("id", "identifier");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());
    JsonArray parameters = new JsonArray();
    parameters.add(parameter);
    request.add("params", parameters);

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/rpc");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(400, response.error().status().code());
  }

  @Test
  public void testMissingId() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", "TimeService.GetTime");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());
    JsonArray parameters = new JsonArray();
    parameters.add(parameter);
    request.add("params", parameters);

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/rpc");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(400, response.error().status().code());
  }

  @Test
  public void testMissingParam() throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("method", ".GetTime");
    request.addProperty("id", "identifier");
    JsonObject parameter = new JsonObject();
    parameter.addProperty("timezone", DateTimeZone.UTC.getID());

    HttpContent httpContent = new ByteArrayContent("application/json",
        new Gson().toJson(request).getBytes(Charsets.UTF_8));

    GenericUrl url = new GenericUrl();
    url.setScheme("http");
    url.setHost("localhost");
    url.setPort(port);
    url.setRawPath("/rpc");

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest httpRequest = requestFactory.buildPostRequest(url, httpContent);

    HttpResponse httpResponse = httpRequest.execute();
    Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, httpResponse.getStatusCode());
    Reader reader = new InputStreamReader(httpResponse.getContent(), Charsets.UTF_8);

    JsonRpcResponse response = JsonRpcResponse.fromJson(new JsonParser().parse(reader)
        .getAsJsonObject());

    Assert.assertTrue(response.isError());
    Assert.assertEquals(400, response.error().status().code());
  }
}
