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

package org.springframework.cloud.commons.util;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.cloud.commons.util.InetUtils.HostInfo;

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
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertNotNull(utils.findFirstNonLoopbackHostInfo());
		}
	}

	@Test
	public void testGetFirstNonLoopbackAddress() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertNotNull(utils.findFirstNonLoopbackAddress());
		}
	}

	@Test
	public void testConvert() throws Exception {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertNotNull(utils.convertAddress(InetAddress.getByName("localhost")));
		}
	}

	@Test
	public void testHostInfo() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			HostInfo info = utils.findFirstNonLoopbackHostInfo();
			assertNotNull(info.getIpAddressAsInt());
		}
	}

	@Test
	public void testIgnoreInterface() {
		InetUtilsProperties properties = new InetUtilsProperties();
		// These interfaces are not usable for "outside" communication, so they're
		// probably not a good fit for the simple strategy of getting the first "usable"
		// interface.
		// https://docs.docker.com/v1.7/articles/networking/
		properties.setIgnoredInterfaces(Arrays.asList("docker0", "veth.*"));
		try (InetUtils inetUtils = new InetUtils(properties)) {

			assertTrue("docker0 not ignored", inetUtils.ignoreInterface("docker0"));
			assertTrue("vethAQI2QT0 not ignored",
					inetUtils.ignoreInterface("vethAQI2QT"));
			assertFalse("docker1 ignored", inetUtils.ignoreInterface("docker1"));
		}
	}

	@Test
	public void testDefaultIgnoreInterface() {
		try (InetUtils inetUtils = new InetUtils(new InetUtilsProperties())) {
			assertFalse("docker0 ignored", inetUtils.ignoreInterface("docker0"));
		}
	}

	@Test
	public void testSiteLocalAddresses() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setUseOnlySiteLocalInterfaces(true);

		try (InetUtils utils = new InetUtils(properties)) {
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1")));
			assertFalse(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1")));
		}
	}
	
	@Test
	public void testPreferredNetworksRegex() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Arrays.asList("192.168.*", "10.0.*"));

		try (InetUtils utils = new InetUtils(properties)) {
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1")));
			assertFalse(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1")));
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1")));
			assertFalse(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1")));
		}
	}
	
	@Test
	public void testPreferredNetworksSimple() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Arrays.asList("192", "10.0"));

		try (InetUtils utils = new InetUtils(properties)) {
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1")));
			assertFalse(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1")));
			assertFalse(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1")));
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1")));
		}
	}

	@Test
	public void testPreferredNetworksListIsEmpty() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Collections.emptyList());
		try (InetUtils utils = new InetUtils(properties)) {
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1")));
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1")));
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1")));
			assertTrue(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1")));
		}
	}
}
