/**
 * Copyright (C) 2014 Turn Inc.  All Rights Reserved.
 * Proprietary and confidential.
 */
package io.soliton.protobuf;

import io.netty.buffer.ByteBuf;

/**
 * Utility methods for {@link io.netty.buffer.ByteBuf}s.
 *
 * @author Peter Foldes (peter.foldes@gmail.com)
 */
public class ByteBufUtil {

  public static byte[] getBytes(ByteBuf buffer) {
	  byte[] bytes;
	  // Not all ByteBuf implementation is backed by a byte array, so check
	  if (buffer.hasArray()) {
		  bytes = buffer.array();
	  } else {
		  bytes = new byte[buffer.readableBytes()];
		  buffer.getBytes(buffer.readerIndex(), bytes);
	  }
    return bytes;
  }

}
