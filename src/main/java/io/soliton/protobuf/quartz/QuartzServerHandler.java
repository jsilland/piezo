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

package io.soliton.protobuf.quartz;

import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.EnvelopeServerHandler;
import io.soliton.protobuf.ServiceGroup;
import io.soliton.protobuf.ServerLogger;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Server-side handler in charge of decoding requests and dispatching them
 * to individual services.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class QuartzServerHandler extends EnvelopeServerHandler<HttpRequest, HttpResponse> {

  private final String path;

  /**
   * Creates new handler that will dispatch requests to the services registered
   * in the given service group.
   *
   * @param serviceGroup the group of services to surface.
   * @param path the HTTP path this handler should handle request on.
   */
  QuartzServerHandler(ServiceGroup serviceGroup, String path, ServerLogger serverLogger) {
    super(serviceGroup, serverLogger);
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Envelope convertRequest(HttpRequest httpRequest) throws RequestConversionException {
    if (!(httpRequest instanceof HttpContent)) {
      throw new RequestConversionException(httpRequest);
    }

    HttpContent content = (HttpContent) httpRequest;
    try {
      return Envelope.PARSER.parseFrom(content.content().array());
    } catch (InvalidProtocolBufferException ipbe) {
      throw new RequestConversionException(httpRequest, ipbe);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(HttpRequest request) {
    try {
      return new URI(request.getUri()).getPath().startsWith(path);
    } catch (URISyntaxException e) {
      logger.warning("Cannot validate URL path, skipping request");
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected HttpResponse convertResponse(Envelope response) {
    ByteBuf responseBuffer = Unpooled.buffer();
    try {
      OutputStream outputStream = new ByteBufOutputStream(responseBuffer);
      response.writeTo(outputStream);
      outputStream.flush();
    } catch (IOException e) {
      // Deliberately ignored, as the underlying operation doesn't involve I/O
    }

    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK, responseBuffer);
    httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, responseBuffer.readableBytes());
    httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, QuartzProtocol.CONTENT_TYPE);
    return httpResponse;
  }
}
