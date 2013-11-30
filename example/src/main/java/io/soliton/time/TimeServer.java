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
package io.soliton.time;

import io.soliton.protobuf.quartz.QuartzServer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Trivial implementation of a time service.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TimeServer implements Time.TimeService.Interface {

  @Parameter(names = "--port", description = "TCP port the server should bind to")
  private Integer port = 10000;

  @Override
  public ListenableFuture<Time.TimeResponse> getTime(Time.TimeRequest request) {
    DateTimeZone timeZone = DateTimeZone.forID(request.getTimezone());
    DateTime now = new DateTime(timeZone);
    Time.TimeResponse.Builder response = Time.TimeResponse.newBuilder();
    return Futures.immediateFuture(response.setTime(now.getMillis()).build());
  }

  public static void main(String... args) throws Exception {
    TimeServer timeServer = new TimeServer();
    new JCommander(timeServer, args);
    QuartzServer server = QuartzServer.newServer(timeServer.port).build();
    server.serviceGroup().addService(Time.TimeService.newService(new TimeServer()));
    server.start();
  }
}
