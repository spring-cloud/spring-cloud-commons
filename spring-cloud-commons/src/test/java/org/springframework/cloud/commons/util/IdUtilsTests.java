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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 */
public class IdUtilsTests {

	public static final String DEFAULT_ID = "id1";

	private MockEnvironment env;

	@Before
	public void setup() {
		this.env = new MockEnvironment();
	}

	@After
	public void destroy() {
		this.env = null;
	}

	@Test
	public void emptyEnvironmentWorks() {
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(instanceId).as("instanceId was not null").isNull();
	}

	@Test
	public void vcapInstanceIdWorks() {
		this.env.setProperty("vcap.application.instance_id", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID).isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void hostnameWorks() {
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID).isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void appNameWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID).isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void hostnameAndAppNameWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID + "2");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(instanceId).as("instanceId was wrong")
				.isEqualTo(DEFAULT_ID + "2" + ":" + DEFAULT_ID);
	}

	@Test
	public void instanceIdWorks() {
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID).isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void portWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID).isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void appNameAndPortWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then(DEFAULT_ID + ":80").isEqualTo(instanceId).as("instanceId was wrong");
	}

	@Test
	public void fullWorks() {
		this.env.setProperty("spring.cloud.client.hostname", "myhost");
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		then("myhost:" + DEFAULT_ID + ":80").isEqualTo(instanceId)
				.as("instanceId was wrong");
	}

	@Test
	public void testUnresolvedServiceId() {
		then(IdUtils.DEFAULT_SERVICE_ID_STRING)
				.isEqualTo(IdUtils.getUnresolvedServiceId());
	}

	@Test
	public void testServiceIdDefaults() {
		this.env.setProperty("cachedrandom.application.value", "123abc");
		then("application:0:123abc").isEqualTo(IdUtils.getResolvedServiceId(this.env));
	}

	@Test
	public void testVCAPServiceId() {
		env.setProperty("vcap.application.name", "vcapname");
		env.setProperty("vcap.application.instance_index", "vcapindex");
		env.setProperty("vcap.application.instance_id", "vcapid");
		then("vcapname:vcapindex:vcapid").isEqualTo(IdUtils.getResolvedServiceId(env));
	}

	@Test
	public void testSpringServiceId() {
		env.setProperty("spring.application.name", "springname");
		env.setProperty("spring.application.index", "springindex");
		env.setProperty("cachedrandom.springname.value", "123abc");
		then("springname:springindex:123abc")
				.isEqualTo(IdUtils.getResolvedServiceId(env));
	}

	@Test
	public void testServerPortServiceId() {
		env.setProperty("spring.application.name", "springname");
		env.setProperty("server.port", "1234");
		env.setProperty("cachedrandom.springname.value", "123abc");
		then("springname:1234:123abc").isEqualTo(IdUtils.getResolvedServiceId(env));
	}

}
