/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
		then(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void relaxedBinding() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("FOO_TEXT: {cipher}bar").applyTo(context.getEnvironment(),
				TestPropertyValues.Type.SYSTEM_ENVIRONMENT);
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo.text")).isEqualTo("bar");
	}

	@Test
	public void propertySourcesOrderedCorrectly() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override",
						Collections.<String, Object>singletonMap("foo", "{cipher}spam")));
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
	}

	@Test
	public void propertySourcesOrderedCorrectlyWithUnencryptedOverrides() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override",
						Collections.<String, Object>singletonMap("foo", "spam")));
		this.listener.initialize(context);
		assertEquals("spam", context.getEnvironment().getProperty("foo"));
	}

	@Test
	public void decryptCipherKeyTwice() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		TestPropertyValues.of("foo2: {cipher}bar2").applyTo(context);
		this.listener.initialize(context);
		assertEquals("bar", context.getEnvironment().getProperty("foo"));
		assertEquals("bar2", context.getEnvironment().getProperty("foo2"));
	}

	@Test(expected = IllegalStateException.class)
	public void errorOnDecrypt() {
		this.listener = new EnvironmentDecryptApplicationInitializer(
				Encryptors.text("deadbeef", "AFFE37"));
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
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
		then(context.getEnvironment().getProperty("foo")).isEqualTo("");
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

		then(context.getEnvironment().getProperty("mine[0].someValue")).isEqualTo("Foo");
		then(context.getEnvironment().getProperty("mine[0].someKey")).isEqualTo("Foo0");
		then(context.getEnvironment().getProperty("mine[1].someValue")).isEqualTo("Bar");
		then(context.getEnvironment().getProperty("mine[1].someKey")).isEqualTo("Bar1");
		then(context.getEnvironment().getProperty("yours[0].someValue"))
				.isEqualTo("yourFoo");
		then(context.getEnvironment().getProperty("yours[1].someValue"))
				.isEqualTo("yourBar");

		MutablePropertySources propertySources = context.getEnvironment()
				.getPropertySources();
		PropertySource<Map<?, ?>> decrypted = (PropertySource<Map<?, ?>>) propertySources
				.get(DECRYPTED_PROPERTY_SOURCE_NAME);
		then(decrypted.getSource().size()).as("decrypted property source had wrong size")
				.isEqualTo(4);
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

		then(ctx.getEnvironment().getProperty("key")).isEqualTo("value");
	}

	@Test
	public void testDecryptCompositePropertySource() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				Encryptors.noOpText());

		CompositePropertySource cps = new CompositePropertySource("testCPS");
		Map<String, Object> map1 = new HashMap<>();
		map1.put("key1", "{cipher}value1b");
		map1.put("key2", "value2b");
		cps.addPropertySource(new MapPropertySource("profile1", map1));
		Map<String, Object> map2 = new HashMap<>();
		map2.put("key1", "{cipher}value1");
		map2.put("key2", "value2");
		map1.put("key3", "value3");
		cps.addPropertySource(new MapPropertySource("profile2", map2));
		// add non-enumerable property source that will fail cps.getPropertyNames()
		cps.addPropertySource(mock(PropertySource.class));
		ctx.getEnvironment().getPropertySources().addLast(cps);

		initializer.initialize(ctx);
		// validate behaviour with encryption
		assertEquals("value1b", ctx.getEnvironment().getProperty("key1"));
		// validate behaviour without encryption
		assertEquals("value2b", ctx.getEnvironment().getProperty("key2"));
		// validate behaviour without override
		assertEquals("value3", ctx.getEnvironment().getProperty("key3"));
	}

	@Test
	public void testOnlyDecryptIfNotOverridden() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TextEncryptor encryptor = mock(TextEncryptor.class);
		when(encryptor.decrypt("bar2")).thenReturn("bar2");
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				encryptor);
		TestPropertyValues.of("foo: {cipher}bar", "foo2: {cipher}bar2").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override",
						Collections.<String, Object>singletonMap("foo", "spam")));
		initializer.initialize(context);
		assertEquals("spam", context.getEnvironment().getProperty("foo"));
		assertEquals("bar2", context.getEnvironment().getProperty("foo2"));
		verify(encryptor).decrypt("bar2");
		verifyNoMoreInteractions(encryptor);
	}

}
