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

package org.springframework.cloud.commons.util;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.cloud.commons.util.InetUtils.HostInfo;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Dave Syer
 *
 */
public class InetUtilsTests {

	@Test
	public void testGetFirstNonLoopbackHostInfo() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			then(utils.findFirstNonLoopbackHostInfo()).isNotNull();
		}
	}

	@Test
	public void testGetFirstNonLoopbackAddress() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			then(utils.findFirstNonLoopbackAddress()).isNotNull();
		}
	}

	@Test
	public void testConvert() throws Exception {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			then(utils.convertAddress(InetAddress.getByName("localhost"))).isNotNull();
		}
	}

	@Test
	public void testHostInfo() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			HostInfo info = utils.findFirstNonLoopbackHostInfo();
			then(info.getIpAddressAsInt()).isNotNull();
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

			then(inetUtils.ignoreInterface("docker0")).isTrue().as("docker0 not ignored");
			then(inetUtils.ignoreInterface("vethAQI2QT")).as("vethAQI2QT0 not ignored")
					.isTrue();
			then(inetUtils.ignoreInterface("docker1")).as("docker1 ignored").isFalse();
		}
	}

	@Test
	public void testDefaultIgnoreInterface() {
		try (InetUtils inetUtils = new InetUtils(new InetUtilsProperties())) {
			then(inetUtils.ignoreInterface("docker0")).as("docker0 ignored").isFalse();
		}
	}

	@Test
	public void testSiteLocalAddresses() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setUseOnlySiteLocalInterfaces(true);

		try (InetUtils utils = new InetUtils(properties)) {
			then(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
		}
	}

	@Test
	public void testPreferredNetworksRegex() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Arrays.asList("192.168.*", "10.0.*"));

		try (InetUtils utils = new InetUtils(properties)) {
			then(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
			then(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1")))
					.isFalse();
		}
	}

	@Test
	public void testPreferredNetworksSimple() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Arrays.asList("192", "10.0"));

		try (InetUtils utils = new InetUtils(properties)) {
			then(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
			then(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1")))
					.isFalse();
			then(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
		}
	}

	@Test
	public void testPreferredNetworksListIsEmpty() throws Exception {
		InetUtilsProperties properties = new InetUtilsProperties();
		properties.setPreferredNetworks(Collections.emptyList());
		try (InetUtils utils = new InetUtils(properties)) {
			then(utils.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("10.255.10.1"))).isTrue();
			then(utils.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
		}
	}

}
