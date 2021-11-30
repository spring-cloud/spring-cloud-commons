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

package org.springframework.cloud.context.scope.refresh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeListBindingIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = TestConfiguration.class, properties = { "test.messages[0]=one", "test.messages[1]=two" })
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
		then("[one, two]").isEqualTo(this.properties.getMessages().toString());
		then(this.properties instanceof Advised).isTrue();
		TestPropertyValues.of("test.messages[0]:foo").applyTo(this.environment);
		this.scope.refreshAll();
		then(this.properties.getMessages().toString()).isEqualTo("[foo]");
	}

	@Test
	@DirtiesContext
	public void testReplaceProperties() throws Exception {
		then("[one, two]").isEqualTo(this.properties.getMessages().toString());
		then(this.properties instanceof Advised).isTrue();
		Map<String, Object> map = findTestProperties();
		map.clear();
		TestPropertyValues.of("test.messages[0]:foo").applyTo(this.environment);
		this.scope.refreshAll();
		then(this.properties.getMessages().toString()).isEqualTo("[foo]");
	}

	private Map<String, Object> findTestProperties() {
		for (PropertySource<?> source : this.environment.getPropertySources()) {
			if (source.getName().toLowerCase().contains("test")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) source.getSource();
				return map;
			}
		}
		throw new IllegalStateException("Could not find test property source");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ RefreshAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		@RefreshScope
		protected TestProperties properties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties("test")
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
