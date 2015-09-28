/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.util;

import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.Test;
import org.springframework.cloud.util.InetUtils.HostInfo;

/**
 * @author Dave Syer
 *
 */
public class InetUtilsTests {

	@Test
	public void testGetFirstNonLoopbackHostInfo() {
		assertNotNull(InetUtils.getFirstNonLoopbackHostInfo());
	}

	@Test
	public void testGetFirstNonLoopbackAddress() {
		assertNotNull(InetUtils.getFirstNonLoopbackAddress());
	}

	@Test
	public void testConvert() throws Exception {
		assertNotNull(InetUtils.convert(InetAddress.getByName("localhost")));
	}

	@Test
	public void testHostInfo() throws Exception {
		HostInfo info = InetUtils.getFirstNonLoopbackHostInfo();
		assertNotNull(info.getIpAddressAsInt());
	}

}
