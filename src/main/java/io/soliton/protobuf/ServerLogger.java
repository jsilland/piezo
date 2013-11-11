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

import com.google.protobuf.Message;

/**
 * Hook interface to allow plugging-in an arbitrary white-box monitoring
 * framework in Piezo.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface ServerLogger {

  /**
   * Invoked when a method call was decoded and is about to be invoked.
   *
   * @param service the service to which the method belongs
   * @param method the method
   */
  void logMethodCall(Service service, ServerMethod<? extends Message, ? extends Message> method);

  /**
   * Invoked when a method invoked has failed.
   *
   * @param serverMethod the method whose invocation failed
   * @param throwable the cause of the failure
   */
  void logServerFailure(ServerMethod<?, ?> serverMethod, Throwable throwable);

  /**
   * Invoked when an error occurred while returning daat to the client.
   *
   * @param serverMethod the method whose response was being sent
   * @param cause the cause of the failure
   */
  void logLinkFailure(ServerMethod<?, ?> serverMethod, Throwable cause);

  /**
   * Invoked when a server has successfully returned a response to the client
   *
   * @param serverMethod the method that was invoked
   */
  void logServerSuccess(ServerMethod<?, ?> serverMethod);

  /**
   * Invoked when the server received a request for an unknown service
   *
   * @param service the name of the received service
   */
  void logUnknownService(Service service);

  /**
   * Logged when the request sent by the client cannot be decoded
   *
   * @param throwable the cause of the error
   */
  void logClientError(Throwable throwable);

  /**
   * Invoked when the method sent by the client is unknown
   *
   * @param service the decoded service
   * @param method the name of the unknown method
   */
  void logUnknownMethod(Service service, String method);
}
