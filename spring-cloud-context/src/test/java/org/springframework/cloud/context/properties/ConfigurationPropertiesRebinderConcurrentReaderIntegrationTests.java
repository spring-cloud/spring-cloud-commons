/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.context.properties;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * A reader of a {@code @ConfigurationProperties} bean must never observe its unbound,
 * class-level defaults while {@link ContextRefresher#refreshEnvironment()} rebinds it,
 * even when nothing in the {@code Environment} changes between refreshes (gh-1727).
 *
 * @author Ryan Baxter
 */
public class ConfigurationPropertiesRebinderConcurrentReaderIntegrationTests {

	private static final int CONFIGURED_PARALLELISM = 4;

	@Test
	public void readersNeverSeeUnboundDefaultsDuringRefresh() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
			.web(WebApplicationType.NONE)
			.run("--demo.parallelism=" + CONFIGURED_PARALLELISM, "--demo.timeout.connect=7")) {
			DemoProperties properties = context.getBean(DemoProperties.class);
			ContextRefresher refresher = context.getBean(ContextRefresher.class);
			AtomicBoolean running = new AtomicBoolean(true);
			AtomicLong reads = new AtomicLong();
			AtomicLong badReads = new AtomicLong();
			AtomicLong failedReads = new AtomicLong();
			Thread reader = new Thread(() -> {
				while (running.get()) {
					reads.incrementAndGet();
					try {
						if (properties.getParallelism() != CONFIGURED_PARALLELISM
								|| properties.getTimeout().getConnect() != 7) {
							badReads.incrementAndGet();
						}
					}
					catch (NullPointerException ex) {
						failedReads.incrementAndGet();
					}
				}
			});
			reader.start();
			try {
				for (int i = 0; i < 500; i++) {
					// nothing changes between calls, yet every call rebinds
					refresher.refreshEnvironment();
				}
			}
			finally {
				running.set(false);
				reader.join(10_000);
			}
			then(reads.get()).isGreaterThan(0);
			then(failedReads.get()).as("reads that threw NullPointerException").isZero();
			then(badReads.get()).as("reads that saw an unbound default").isZero();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DemoProperties.class)
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

	@ConfigurationProperties("demo")
	protected static class DemoProperties {

		// left uninitialized so a reset to defaults is visible
		private int parallelism;

		private Timeout timeout;

		public int getParallelism() {
			return this.parallelism;
		}

		public void setParallelism(int parallelism) {
			this.parallelism = parallelism;
		}

		public Timeout getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Timeout timeout) {
			this.timeout = timeout;
		}

		protected static class Timeout {

			private int connect;

			public int getConnect() {
				return this.connect;
			}

			public void setConnect(int connect) {
				this.connect = connect;
			}

		}

	}

}
