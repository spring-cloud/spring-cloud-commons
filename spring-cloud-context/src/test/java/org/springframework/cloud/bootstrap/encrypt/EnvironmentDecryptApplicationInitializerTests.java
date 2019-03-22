/*
 * Copyright 2012-2019 the original author or authors.
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
import static org.mockito.ArgumentMatchers.anyString;
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
		String expected = "always";
		TextEncryptor textEncryptor = mock(TextEncryptor.class);
		when(textEncryptor.decrypt(anyString())).thenReturn(expected);

		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				textEncryptor);

		MapPropertySource source = new MapPropertySource("nobody",
				Collections.singletonMap("key", "{cipher}value"));
		CompositePropertySource cps = mock(CompositePropertySource.class);
		when(cps.getPropertyNames()).thenReturn(source.getPropertyNames());
		when(cps.getPropertySources()).thenReturn(Collections.singleton(source));
		ctx.getEnvironment().getPropertySources().addLast(cps);

		initializer.initialize(ctx);
		then(ctx.getEnvironment().getProperty("key")).isEqualTo(expected);
	}

}
