/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.cloud.bootstrap.encrypt;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.security.crypto.encrypt.Encryptors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer.DECRYPTED_PROPERTY_SOURCE_NAME;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class EnvironmentDecryptApplicationInitializerTests {

	private EnvironmentDecryptApplicationInitializer listener = new EnvironmentDecryptApplicationInitializer(
			Encryptors.noOpText());

	@Test
	public void decryptCipherKey() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo"));
	}

	@Test
	public void relaxedBinding() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("FOO_TEXT: {cipher}bar").applyTo(context.getEnvironment(),
				TestPropertyValues.Type.SYSTEM_ENVIRONMENT);
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo.text"));
	}

	@Test
	public void propertySourcesOrderedCorrectly() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override",
						Collections.<String, Object>singletonMap("foo", "{cipher}spam")));
		this.listener.initialize(context);
		assertEquals("spam", context.getEnvironment().getProperty("foo"));
	}

	@Test(expected = IllegalStateException.class)
	public void errorOnDecrypt() {
		this.listener = new EnvironmentDecryptApplicationInitializer(
				Encryptors.text("deadbeef", "AFFE37"));
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo"));
	}

	@Test
	public void errorOnDecryptWithEmpty() {
		this.listener = new EnvironmentDecryptApplicationInitializer(
				Encryptors.text("deadbeef", "AFFE37"));
		this.listener.setFailOnError(false);
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		// Empty is safest fallback for undecryptable cipher
		assertEquals("", context.getEnvironment().getProperty("foo"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void indexedPropertiesCopied() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		// tests that collections in another property source don't get copied into
		// "decrypted" property source
		TestPropertyValues
				.of("yours[0].someValue: yourFoo", "yours[1].someValue: yourBar")
				.applyTo(context);

		// collection with some encrypted keys and some not encrypted
		TestPropertyValues
				.of("mine[0].someValue: Foo", "mine[0].someKey: {cipher}Foo0",
						"mine[1].someValue: Bar", "mine[1].someKey: {cipher}Bar1",
						"nonindexed: nonindexval")
				.applyTo(context.getEnvironment(), Type.MAP, "combinedTest");
		this.listener.initialize(context);

		assertEquals("Foo", context.getEnvironment().getProperty("mine[0].someValue"));
		assertEquals("Foo0", context.getEnvironment().getProperty("mine[0].someKey"));
		assertEquals("Bar", context.getEnvironment().getProperty("mine[1].someValue"));
		assertEquals("Bar1", context.getEnvironment().getProperty("mine[1].someKey"));
		assertEquals("yourFoo",
				context.getEnvironment().getProperty("yours[0].someValue"));
		assertEquals("yourBar",
				context.getEnvironment().getProperty("yours[1].someValue"));

		MutablePropertySources propertySources = context.getEnvironment()
				.getPropertySources();
		PropertySource<Map<?, ?>> decrypted = (PropertySource<Map<?, ?>>) propertySources
				.get(DECRYPTED_PROPERTY_SOURCE_NAME);
		assertThat("decrypted property source had wrong size",
				decrypted.getSource().size(), is(4));
	}

	@Test
	public void testDecryptNonStandardParent() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				Encryptors.noOpText());

		TestPropertyValues.of("key:{cipher}value").applyTo(ctx);

		ApplicationContext ctxParent = mock(ApplicationContext.class);
		when(ctxParent.getEnvironment()).thenReturn(mock(Environment.class));

		ctx.setParent(ctxParent);

		initializer.initialize(ctx);

		assertEquals("value", ctx.getEnvironment().getProperty("key"));
	}

}
