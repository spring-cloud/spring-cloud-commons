/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.bootstrap.BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME;
import static org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer.DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME;
import static org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer.DECRYPTED_PROPERTY_SOURCE_NAME;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 * @author Tim Ysewyn
 */
@ExtendWith(OutputCaptureExtension.class)
public class EnvironmentDecryptApplicationInitializerTests {

	private EnvironmentDecryptApplicationInitializer listener = new EnvironmentDecryptApplicationInitializer(
			Encryptors.noOpText());

	@Test
	public void decryptCipherKey() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void relaxedBinding() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "FOO_TEXT: {cipher}bar")
				.applyTo(context.getEnvironment(), TestPropertyValues.Type.SYSTEM_ENVIRONMENT);
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo.text")).isEqualTo("bar");
	}

	@Test
	public void propertySourcesOrderedCorrectly() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override", Collections.singletonMap("foo", "{cipher}spam")));
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
	}

	@Test
	public void errorOnDecrypt(CapturedOutput output) {
		this.listener = new EnvironmentDecryptApplicationInitializer(Encryptors.text("deadbeef", "AFFE37"));
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar").applyTo(context);
		// catch IllegalStateException and verify
		try {
			this.listener.initialize(context);
		}
		catch (Exception e) {
			then(e).isInstanceOf(IllegalStateException.class);
		}
		// Assert logs contain warning even when exception thrown
		String sysOutput = output.toString();
		then(sysOutput).contains("Cannot decrypt: key=foo");
	}

	@Test
	public void errorOnDecryptWithEmpty(CapturedOutput output) {
		this.listener = new EnvironmentDecryptApplicationInitializer(Encryptors.text("deadbeef", "AFFE37"));
		this.listener.setFailOnError(false);
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar").applyTo(context);
		this.listener.initialize(context);
		// Assert logs contain warning
		String sysOutput = output.toString();
		then(sysOutput).contains("Cannot decrypt: key=foo");
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
				.of("spring.cloud.bootstrap.enabled=true", "yours[0].someValue: yourFoo", "yours[1].someValue: yourBar")
				.applyTo(context);

		// collection with some encrypted keys and some not encrypted
		TestPropertyValues
				.of("mine[0].someValue: Foo", "mine[0].someKey: {cipher}Foo0", "mine[1].someValue: Bar",
						"mine[1].someKey: {cipher}Bar1", "nonindexed: nonindexval")
				.applyTo(context.getEnvironment(), Type.MAP, "combinedTest");
		this.listener.initialize(context);

		then(context.getEnvironment().getProperty("mine[0].someValue")).isEqualTo("Foo");
		then(context.getEnvironment().getProperty("mine[0].someKey")).isEqualTo("Foo0");
		then(context.getEnvironment().getProperty("mine[1].someValue")).isEqualTo("Bar");
		then(context.getEnvironment().getProperty("mine[1].someKey")).isEqualTo("Bar1");
		then(context.getEnvironment().getProperty("yours[0].someValue")).isEqualTo("yourFoo");
		then(context.getEnvironment().getProperty("yours[1].someValue")).isEqualTo("yourBar");

		MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
		PropertySource<Map<?, ?>> decrypted = (PropertySource<Map<?, ?>>) propertySources
				.get(DECRYPTED_PROPERTY_SOURCE_NAME);
		then(decrypted.getSource().size()).as("decrypted property source had wrong size").isEqualTo(4);
	}

	@Test
	public void testDecryptNonStandardParent() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				Encryptors.noOpText());

		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "key:{cipher}value").applyTo(ctx);

		ApplicationContext ctxParent = mock(ApplicationContext.class);
		when(ctxParent.getEnvironment()).thenReturn(mock(Environment.class));

		ctx.setParent(ctxParent);

		initializer.initialize(ctx);

		then(ctx.getEnvironment().getProperty("key")).isEqualTo("value");
	}

	@Test
	public void testDecryptCompositePropertySource() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true").applyTo(ctx);
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(
				Encryptors.noOpText());

		MapPropertySource devProfile = new MapPropertySource("dev-profile",
				Collections.singletonMap("key", "{cipher}value1"));

		MapPropertySource defaultProfile = new MapPropertySource("default-profile",
				Collections.singletonMap("key", "{cipher}value2"));

		CompositePropertySource cps = mock(CompositePropertySource.class);
		when(cps.getName()).thenReturn("mock-composite-source");
		when(cps.getPropertyNames()).thenReturn(devProfile.getPropertyNames());
		when(cps.getPropertySources()).thenReturn(Arrays.asList(devProfile, defaultProfile));
		ctx.getEnvironment().getPropertySources().addLast(cps);

		initializer.initialize(ctx);
		then(ctx.getEnvironment().getProperty("key")).isEqualTo("value1");
	}

	@Test
	public void propertySourcesOrderedCorrectlyWithUnencryptedOverrides() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar").applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override", Collections.singletonMap("foo", "spam")));
		this.listener.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
	}

	@Test
	public void doNotDecryptBootstrapTwice() {
		TextEncryptor encryptor = mock(TextEncryptor.class);
		when(encryptor.decrypt("bar")).thenReturn("bar");
		when(encryptor.decrypt("bar2")).thenReturn("bar2");
		when(encryptor.decrypt("bar3")).thenReturn("bar3");
		when(encryptor.decrypt("baz")).thenReturn("baz");

		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(encryptor);

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true").applyTo(context);
		CompositePropertySource bootstrap = new CompositePropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME);
		bootstrap.addPropertySource(
				new MapPropertySource("configService", Collections.singletonMap("foo", "{cipher}bar")));
		context.getEnvironment().getPropertySources().addFirst(bootstrap);

		Map<String, Object> props = new HashMap<>();
		props.put("foo2", "{cipher}bar2");
		props.put("bar", "{cipher}baz");
		context.getEnvironment().getPropertySources().addAfter(BOOTSTRAP_PROPERTY_SOURCE_NAME,
				new MapPropertySource("remote", props));

		initializer.initialize(context);

		// Simulate retrieval of new properties via Spring Cloud Config
		props.put("foo2", "{cipher}bar3");
		context.getEnvironment().getPropertySources().replace("remote", new MapPropertySource("remote", props));

		initializer.initialize(context);

		verify(encryptor).decrypt("bar");
		verify(encryptor).decrypt("bar2");
		verify(encryptor).decrypt("bar3");
		verify(encryptor, times(2)).decrypt("baz");

		// Check if all encrypted properties are still decrypted
		PropertySource<?> decryptedBootstrap = context.getEnvironment().getPropertySources()
				.get(DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME);
		then(decryptedBootstrap.getProperty("foo")).isEqualTo("bar");

		PropertySource<?> decrypted = context.getEnvironment().getPropertySources().get(DECRYPTED_PROPERTY_SOURCE_NAME);
		then(decrypted.getProperty("foo2")).isEqualTo("bar3");
		then(decrypted.getProperty("bar")).isEqualTo("baz");
	}

	@Test
	public void testOnlyDecryptIfNotOverridden() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TextEncryptor encryptor = mock(TextEncryptor.class);
		when(encryptor.decrypt("bar2")).thenReturn("bar2");
		EnvironmentDecryptApplicationInitializer initializer = new EnvironmentDecryptApplicationInitializer(encryptor);
		TestPropertyValues.of("spring.cloud.bootstrap.enabled=true", "foo: {cipher}bar", "foo2: {cipher}bar2")
				.applyTo(context);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test_override", Collections.singletonMap("foo", "spam")));
		initializer.initialize(context);
		then(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
		then(context.getEnvironment().getProperty("foo2")).isEqualTo("bar2");
		verify(encryptor).decrypt("bar2");
		verifyNoMoreInteractions(encryptor);
	}

}
