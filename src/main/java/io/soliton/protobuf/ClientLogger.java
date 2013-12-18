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
public interface ClientLogger {

  /**
   * Invoked upon a client-side method being called.
   *
   * @param method the method being invoked
   */
  public void logMethodCall(ClientMethod<?> method);

  /**
   * Invoked upon an error happening on the client-side, e.g. failed method
   * call because of a broken link.
   *
   * @param method the method that was called when the error happened
   * @param cause the underlying cause of the error
   */
  public void logClientError(ClientMethod<?> method, Throwable cause);

  /**
   * Invoked when an error was returned from the server.
   *
   * @param serviceName
   * @param methodName
   * @param cause the underlying cause of the error, as returned by the server
   */
  public void logServerError(String serviceName, String methodName, Throwable cause);

  /**
   * Invoked upon an error occurring while sending a method call to the server.
   *
   * @param method the method that was invoked
   * @param cause the cause of the error
   * @param <O> the response type of the method
   */
  public <O extends Message> void logLinkError(ClientMethod<O> method, Throwable cause);

  /**
   * Invoked when a method invocation returned a successful response.
   *
   * @param clientMethod the method that was invoked
   * @param <O> the return type of the method
   */
  public <O extends Message> void logSuccess(ClientMethod<O> clientMethod);
}
