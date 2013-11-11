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

import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.EnvelopeClientHandler;

/**
 * Client handler in charge of decoding the server's response and dispatching
 * it to the relevant client.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class RpcClientHandler extends EnvelopeClientHandler<Envelope, Envelope> {

  @Override
  public Envelope convertRequest(Envelope request) {
    return request;
  }

  @Override
  public Envelope convertResponse(Envelope response) throws ResponseConversionException {
    return response;
  }
}
