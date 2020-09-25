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

package org.springframework.cloud.bootstrap.encrypt;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-security-rsa*.jar" })
public class RsaDisabledTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		this.context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(EncryptionBootstrapConfiguration.class).web(WebApplicationType.NONE)
				.properties("encrypt.key:mykey", "encrypt.rsa.strong:true", "encrypt.rsa.salt:foobar").run();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testLoadBalancedRetryFactoryBean() throws Exception {
		Map<String, RsaProperties> properties = this.context.getBeansOfType(RsaProperties.class);
		then(properties.values()).hasSize(0);
	}

}
