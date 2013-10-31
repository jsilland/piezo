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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.soliton.protobuf.testing.TimeRequest;
import io.soliton.protobuf.testing.TimeResponse;
import io.soliton.protobuf.testing.TimeService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Trivial implementation of the test time service.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TimeServer implements TimeService.Interface {

  @Override
  public ListenableFuture<TimeResponse> getTime(TimeRequest request) {
    DateTimeZone timeZone = DateTimeZone.forID(request.getTimezone());
    DateTime now = new DateTime(timeZone);
    TimeResponse.Builder response = TimeResponse.newBuilder();
    return Futures.immediateFuture(response.setTime(now.getMillis()).build());
  }
}
