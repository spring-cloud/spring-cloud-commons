/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery.event;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class for listeners to the {@link HeartbeatEvent}, providing a convenient way to
 * determine if there has been a change in state.
 *
 * @author Dave Syer
 */
public class HeartbeatMonitor {

	private AtomicReference<Object> latestHeartbeat = new AtomicReference<>();

	/**
	 * @param value The latest heartbeat.
	 * @return True if the state changed.
	 */
	public boolean update(Object value) {
		Object last = this.latestHeartbeat.get();
		if (value != null && !value.equals(last)) {
			return this.latestHeartbeat.compareAndSet(last, value);
		}
		return false;
	}

}
