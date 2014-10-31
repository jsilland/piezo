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

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Default implementation of {@link ServiceGroup}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class DefaultServiceGroup implements ServiceGroup {

	private final Map<String, Service> services = Maps.newHashMap();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addService(Service service) {
		Preconditions.checkNotNull(service);
		services.put(service.fullName(), service);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Service lookupByName(String name) {
		return services.get(name);
	}

	@Override
	public String toString() {
		return services.toString();
	}
}
