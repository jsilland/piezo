/**
 * Copyright 2013 Julien Silland
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.soliton.protobuf;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DefaultServiceGroup}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class DefaultServiceGroupTest {

	@Test
	public void testAddAndLookup() {
		Service service = new Service() {

			@Override
			public ServerMethod<? extends Message, ? extends Message> lookup(String name) {
				return null;
			}

			@Override
			public ImmutableMap<String, ServerMethod<? extends Message,
					? extends Message>> methods() {
				return null;
			}

			@Override
			public String shortName() {
				return "Service";
			}

			@Override
			public String fullName() {
				return "proto.package.Service";
			}
		};

		ServiceGroup group = new DefaultServiceGroup();
		Assert.assertNull(group.lookupByName("Service"));
		group.addService(service);
		Assert.assertNull(group.lookupByName("Service"));
		Assert.assertNotNull(group.lookupByName("proto.package.Service"));
	}
}
