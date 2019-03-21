/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.cloud.context.scope.refresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeListBindingIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringApplicationConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest("messages=one,two")
public class RefreshScopeListBindingIntegrationTests {

	@Autowired
	private TestProperties properties;

	@Autowired
	private org.springframework.cloud.context.scope.refresh.RefreshScope scope;

	@Autowired
	private ConfigurableEnvironment environment;

	@Test
	@DirtiesContext
	public void testAppendProperties() throws Exception {
		assertEquals("[one, two]", this.properties.getMessages().toString());
		assertTrue(this.properties instanceof Advised);
		EnvironmentTestUtils.addEnvironment(this.environment, "messages[0]:foo");
		this.scope.refreshAll();
		assertEquals("[foo, two]", this.properties.getMessages().toString());
	}

	@Test
	@DirtiesContext
	public void testReplaceProperties() throws Exception {
		assertEquals("[one, two]", this.properties.getMessages().toString());
		assertTrue(this.properties instanceof Advised);
		@SuppressWarnings("unchecked")
		Map<String,Object> map = (Map<String, Object>) this.environment.getPropertySources().get("integrationTest").getSource();
		map.clear();
		EnvironmentTestUtils.addEnvironment(this.environment, "messages[0]:foo");
		this.scope.refreshAll();
		assertEquals("[foo]", this.properties.getMessages().toString());
	}

	@Configuration
	@EnableConfigurationProperties
	@Import({ RefreshAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		protected TestProperties properties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties
	@ManagedResource
	protected static class TestProperties {

		private List<String> messages = new ArrayList<String>();

		public List<String> getMessages() {
			return this.messages;
		}

		public void setMessages(List<String> messages) {
			this.messages = messages;
		}

	}

}
