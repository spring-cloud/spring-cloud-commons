/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.encrypt.Encryptors;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 *
 */
public class EnvironmentDecryptApplicationListenerTests {

	private EnvironmentDecryptApplicationInitializer listener = new EnvironmentDecryptApplicationInitializer(
			Encryptors.noOpText());

	@Test
	public void decryptCipherKey() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "foo: {cipher}bar");
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo"));
	}

	@Test
	public void propertySourcesOrderedCorrectly() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "foo: {cipher}bar");
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
				"test_override",
				Collections.<String, Object> singletonMap("foo", "{cipher}spam")));
		this.listener.initialize(context);
		assertEquals("spam", context.getEnvironment().getProperty("foo"));
	}

	@Test(expected = IllegalStateException.class)
	public void errorOnDecrypt() {
		this.listener = new EnvironmentDecryptApplicationInitializer(
				Encryptors.text("deadbeef", "AFFE37"));
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "foo: {cipher}bar");
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo"));
	}

	@Test
	public void errorOnDecryptWithEmpty() {
		this.listener = new EnvironmentDecryptApplicationInitializer(
				Encryptors.text("deadbeef", "AFFE37"));
		this.listener.setFailOnError(false);
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "foo: {cipher}bar");
		this.listener.initialize(context);
		// Empty is safest fallback for undecryptable cipher
		assertEquals("", context.getEnvironment().getProperty("foo"));
	}

}
