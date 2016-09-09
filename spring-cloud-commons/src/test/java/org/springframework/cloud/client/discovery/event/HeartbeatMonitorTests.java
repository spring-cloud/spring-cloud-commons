/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery.event;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class HeartbeatMonitorTests {

	private HeartbeatMonitor monitor = new HeartbeatMonitor();

	@Test
	public void onAndOff() {
		assertTrue(this.monitor.update("foo"));
		assertFalse(this.monitor.update("foo"));
	}

	@Test
	public void toggle() {
		assertTrue(this.monitor.update("foo"));
		assertTrue(this.monitor.update("bar"));
	}

	@Test
	public void nullInitialValue() {
		assertFalse(this.monitor.update(null));
	}

	@Test
	public void nullSecondValue() {
		assertTrue(this.monitor.update("foo"));
		assertFalse(this.monitor.update(null));
	}

}
