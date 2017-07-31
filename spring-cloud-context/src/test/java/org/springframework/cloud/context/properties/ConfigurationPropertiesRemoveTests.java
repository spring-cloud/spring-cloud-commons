/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.context.properties;

import org.junit.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * To test behavior of Configuration properties with properties removed
 * 
 * @author Biju Kunjummen
 */

public class ConfigurationPropertiesRemoveTests {

	@Test
	public void shouldBindNewPropertiesToConfigurationPropertiesBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, "testprops.messages[0]=msg0",
				"testprops.messages[1]=msg1", "testprops.mapMessages.key1=val1",
				"testprops.mapMessages.key2=val2");
		ctx.register(TestConfiguration.class);
		ctx.refresh();

		TestProperties testProperties = ctx.getBean(TestProperties.class);

		assertThat(testProperties.getMessages()).containsExactly("msg0", "msg1");
		assertThat(testProperties.getMapMessages()).containsOnlyKeys("key1", "key2");

		EnvironmentTestUtils.addEnvironment(ctx, "testprops.messages[2]=msg2",
				"testprops.mapMessages.key3=val3");

		ctx.publishEvent(new EnvironmentChangeEvent(
				new HashSet<>(Arrays.asList("testprops.messages"))));

		assertThat(testProperties.getMessages()).containsExactly("msg0", "msg1", "msg2");
		assertThat(testProperties.getMapMessages()).containsOnlyKeys("key1", "key2", "key3");
	}

	@Test
	public void shouldRemovePropertiesFromConfigurationPropertiesBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, "testprops.messages[0]=msg0",
				"testprops.messages[1]=msg1", "testprops.mapMessages.key1=val1",
				"testprops.mapMessages.key2=val2");
		ctx.register(TestConfiguration.class);
		ctx.refresh();

		TestProperties testProperties = ctx.getBean(TestProperties.class);

		assertThat(testProperties.getMessages()).containsExactly("msg0", "msg1");
		assertThat(testProperties.getMapMessages()).containsOnlyKeys("key1", "key2");

		removeEnv(ctx.getEnvironment(), "testprops.mapMessages.key2");

		ctx.publishEvent(new EnvironmentChangeEvent(
				new HashSet<>(Arrays.asList("testprops.mapMessages.key2")), EnvironmentChangeEvent.ChangeType.DELETE));

		assertThat(testProperties.getMessages()).containsExactly("msg0", "msg1");
		assertThat(testProperties.getMapMessages()).containsOnlyKeys("key1");
	}

	private void removeEnv(ConfigurableEnvironment environment, String... keys) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = (Map<String, Object>) sources.get("test").getSource();
		for (String key : keys) {
			map.remove(key);
		}
	}

	@Configuration
	@Import({ ConfigurationPropertiesAutoConfiguration.class,
			ConfigurationPropertiesRebinderAutoConfiguration.class})
	protected static class TestConfiguration {

		@Bean
		protected TestProperties properties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties(prefix = "testprops")
	protected static class TestProperties implements PropertiesResettable {
		private List<String> messages;
		private Map<String, String> mapMessages = new HashMap<>();

		public List<String> getMessages() {
			return this.messages;
		}

		public void setMessages(List<String> messages) {
			this.messages = messages;
		}

		public Map<String, String> getMapMessages() {
			return mapMessages;
		}

		public void setMapMessages(Map<String, String> mapMessages) {
			this.mapMessages = mapMessages;
		}

		@Override
		public void reset() {
			this.messages = new ArrayList<>();
			this.mapMessages = new HashMap<>();
		}
	}

}
