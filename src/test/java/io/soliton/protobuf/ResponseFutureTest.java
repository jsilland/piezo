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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.soliton.protobuf.testing.TimeResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link ResponseFuture}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class ResponseFutureTest {

  @Test
  public void testRequestId() {
    Runnable cancel = new Runnable() {
      public void run() {
        // noop
      }
    };
    ResponseFuture<TimeResponse> future = new ResponseFuture<>(1L, cancel, TimeResponse.PARSER);
    Assert.assertEquals(1L, future.requestId());
  }

  @Test
  public void testSetResponse() throws InterruptedException {
    Runnable cancel = new Runnable() {
      public void run() {
        // noop
      }
    };
    final CountDownLatch latch = new CountDownLatch(1);
    FutureCallback<TimeResponse> callback = new FutureCallback<TimeResponse>() {
      @Override
      public void onSuccess(@Nullable TimeResponse result) {
        latch.countDown();
      }

      @Override
      public void onFailure(Throwable t) {
        Assert.fail();
      }
    };
    ResponseFuture<TimeResponse > future = new ResponseFuture<>(1L, cancel, TimeResponse.PARSER);
    Envelope response = Envelope.newBuilder()
        .setPayload(TimeResponse.newBuilder().setTime(12345L).build().toByteString())
        .build();
    Futures.addCallback(future, callback);
    future.setResponse(response);
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  public void testSetResponseWithException() throws InterruptedException {
    Runnable cancel = new Runnable() {
      public void run() {
        // noop
      }
    };
    final CountDownLatch latch = new CountDownLatch(1);
    FutureCallback<TimeResponse> callback = new FutureCallback<TimeResponse>() {
      @Override
      public void onSuccess(@Nullable TimeResponse result) {

      }

      @Override
      public void onFailure(Throwable t) {
        Assert.assertTrue(t.getMessage().contains("OMGWTF"));
        latch.countDown();
      }
    };
    ResponseFuture<TimeResponse > future = new ResponseFuture<>(1L, cancel, TimeResponse.PARSER);
    Control control = Control.newBuilder()
        .setError("OMGWTF")
        .build();
    Envelope response = Envelope.newBuilder()
        .setControl(control)
        .build();
    Futures.addCallback(future, callback);
    future.setResponse(response);
    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  public void testCancel() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable cancel = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    ResponseFuture<TimeResponse > future = new ResponseFuture<>(1L, cancel, TimeResponse.PARSER);
    future.cancel(true);
    latch.await(5, TimeUnit.SECONDS);
  }
}
