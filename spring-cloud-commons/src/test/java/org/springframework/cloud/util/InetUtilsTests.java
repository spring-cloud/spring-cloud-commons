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

import java.net.InetAddress;
import java.util.Arrays;

import org.junit.Test;
import org.springframework.cloud.util.InetUtils.HostInfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 *
 */
public class InetUtilsTests {

	@Test
	public void testGetFirstNonLoopbackHostInfo() {
		assertNotNull(
				new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackHostInfo());
	}

	@Test
	public void testGetFirstNonLoopbackAddress() {
		assertNotNull(
				new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackAddress());
	}

	@Test
	public void testConvert() throws Exception {
		assertNotNull(new InetUtils(new InetUtilsProperties())
				.convertAddress(InetAddress.getByName("localhost")));
		assertNotNull(InetUtils.convert(InetAddress.getByName("localhost")));
	}

	@Test
	public void testHostInfo() throws Exception {
		HostInfo info = new InetUtils(new InetUtilsProperties())
				.findFirstNonLoopbackHostInfo();
		assertNotNull(info.getIpAddressAsInt());
	}

	@Test
	public void testIgnoreInterface() {
		InetUtilsProperties properties = new InetUtilsProperties();
		// These interfaces are not usable for "outside" communication, so they're
		// probably not a good fit for the simple strategy of getting the first "usable"
		// interface.
		// https://docs.docker.com/v1.7/articles/networking/
		properties.setIgnoredInterfaces(Arrays.asList("docker0", "veth.*"));
		InetUtils inetUtils = new InetUtils(properties);

		assertTrue("docker0 not ignored", inetUtils.ignoreInterface("docker0"));
		assertTrue("vethAQI2QT0 not ignored", inetUtils.ignoreInterface("vethAQI2QT"));
		assertFalse("docker1 ignored", inetUtils.ignoreInterface("docker1"));

		assertFalse("docker0 ignored",
				new InetUtils(new InetUtilsProperties()).ignoreInterface("docker0"));
	}

}
