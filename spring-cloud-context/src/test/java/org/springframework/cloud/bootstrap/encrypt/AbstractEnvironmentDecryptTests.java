/*
 * Copyright 2013-present the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.bootstrap.encrypt.AbstractEnvironmentDecrypt.DECRYPTED_PROPERTY_SOURCE_NAME;

@ExtendWith(OutputCaptureExtension.class)
public class AbstractEnvironmentDecryptTests {

	private final AbstractEnvironmentDecrypt decryptor = new AbstractEnvironmentDecrypt() {
	};

	private ConfigurableEnvironment environment;

	@BeforeEach
	void setup() {
		environment = new AnnotationConfigApplicationContext().getEnvironment();
	}

	@Test
	void decryptCipherKey() {
		environment.getPropertySources().addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar")));

		decrypt();

		then(environment.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void decryptCipherKeyWithPriority() {
		environment.getPropertySources().addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar")));
		environment.getPropertySources().addFirst(new MapPropertySource("source-2", Map.of("foo", "{cipher}spam")));

		decrypt();
		then(environment.getProperty("foo")).isEqualTo("spam");
	}

	@Test
	void relaxedBinding() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1",
					Map.of("foo.text", "{cipher}foo1", "bar_text", "bar1", "baz[0].text", "baz1")));
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-2",
					Map.of("FOO_TEXT", "{cipher}foo2", "BAR_TEXT", "{cipher}bar2", "BAZ[0].TEXT", "{cipher}baz2")));

		decrypt();

		then(environment.getProperty("foo.text")).isEqualTo("foo2");
		then(environment.getProperty("bar-text")).isEqualTo("bar2");
		then(environment.getProperty("baz[0].text")).isEqualTo("baz2");
	}

	@Test
	void errorOnDecrypt(CapturedOutput output) {
		environment.getPropertySources().addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar")));

		assertThatThrownBy(() -> decrypt(Encryptors.text("deadbeef", "AFFE37")))
			.isInstanceOf(IllegalStateException.class);

		// Assert logs contain warning even when exception thrown
		then(output.toString()).contains("Cannot decrypt: key=foo");
	}

	@Test
	void errorOnDecryptWhenFailOnErrorIsOff(CapturedOutput output) {
		environment.getPropertySources().addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar")));
		decryptor.setFailOnError(false);

		decrypt(Encryptors.text("deadbeef", "AFFE37"));

		// Assert logs contain warning
		then(output.toString()).contains("Cannot decrypt: key=foo");
		// Empty is the safest fallback for undecryptable cipher
		then(environment.getProperty("foo")).isEqualTo("");
	}

	@Test
	void indexedPropertiesAreCopied() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1",
					Map.of("yours[0].someValue", "yourFoo", "yours[1].someValue", "yourBar")));
		// collection with some encrypted keys and some not encrypted
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-2",
					Map.of("mine[0].someValue", "Foo", "mine[0].someKey", "{cipher}Foo0", "mine[1].someValue", "Bar",
							"mine[1].someKey", "{cipher}Bar1", "nonindexed", "nonindexval")));

		decrypt();

		then(environment.getProperty("mine[0].someValue")).isEqualTo("Foo");
		then(environment.getProperty("mine[0].someKey")).isEqualTo("Foo0");
		then(environment.getProperty("mine[1].someValue")).isEqualTo("Bar");
		then(environment.getProperty("mine[1].someKey")).isEqualTo("Bar1");

		then(environment.getProperty("yours[0].someValue")).isEqualTo("yourFoo");
		then(environment.getProperty("yours[1].someValue")).isEqualTo("yourBar");
	}

	@Test
	void indexedPropertiesAreCopiedOnlyIfEncrypted() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1",
					Map.of("a[0]", "a0", "a[1]", "{cipher}a1", "b[0]", "b0", "b[1]", "b1")));
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-2", Map.of("b[0]", "updated-b0", "b[1]", "updated-b1")));

		decrypt();

		then(environment.getProperty("a[0]")).isEqualTo("a0");
		then(environment.getProperty("a[1]")).isEqualTo("a1");

		then(environment.getProperty("b[0]")).isEqualTo("updated-b0");
		then(environment.getProperty("b[1]")).isEqualTo("updated-b1");

		var decryptedPropertySource = environment.getPropertySources().get("decrypted");
		then(decryptedPropertySource).isNotNull();
		var source = decryptedPropertySource.getSource();
		then(source).isInstanceOf(Map.class);
		then(((Map<?, ?>) source).size()).as("decrypted property source had wrong size").isEqualTo(2);
	}

	@Test
	void decryptCompositePropertySource() {
		var cps = new CompositePropertySource("composite-source");
		cps.addPropertySource(new MapPropertySource("dev-profile", Map.of("key", "{cipher}value1")));
		cps.addPropertySource(new MapPropertySource("default-profile", Map.of("key", "{cipher}value2")));
		environment.getPropertySources().addFirst(cps);

		decrypt();
		then(environment.getProperty("key")).isEqualTo("value1");
	}

	@Test
	void propertySourcesOrderedCorrectlyWithUnencryptedOverrides() {
		environment.getPropertySources().addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar")));
		environment.getPropertySources().addFirst(new MapPropertySource("source-2", Map.of("foo", "spam")));

		decrypt();

		then(environment.getProperty("foo")).isEqualTo("spam");
	}

	@Test
	void decryptOnlyIfNotOverridden() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1", Map.of("foo", "{cipher}bar", "foo2", "{cipher}bar2")));
		environment.getPropertySources().addFirst(new MapPropertySource("source-2", Map.of("foo", "spam")));

		decrypt();

		then(environment.getProperty("foo2")).isEqualTo("bar2");
		then(environment.getProperty("foo")).isEqualTo("spam");
	}

	@Test
	void indexedPropertiesAreHandledCorrectly() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1",
					Map.of("list[0].plain", "good", "list[0].cipher", "{cipher}bad")));
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-2", Map.of("list[0].plain", "well", "list[0].cipher", "worse")));

		decrypt();

		then(environment.getProperty("list[0].plain")).isEqualTo("well");
		then(environment.getProperty("list[0].cipher")).isEqualTo("worse");
	}

	@Test
	void anonymousIndexedPropertiesAreHandledCorrectly() {
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-1", Map.of("[0].plain", "good", "[0].cipher", "{cipher}bad")));
		environment.getPropertySources()
			.addFirst(new MapPropertySource("source-2", Map.of("[0].plain", "well", "[0].cipher", "{cipher}worse")));

		decrypt();

		then(environment.getProperty("[0].plain")).isEqualTo("well");
		then(environment.getProperty("[0].cipher")).isEqualTo("worse");
	}

	private void decrypt() {
		decrypt(Encryptors.noOpText());
	}

	private void decrypt(TextEncryptor encryptor) {
		var decrypted = decryptor.decrypt(encryptor, environment.getPropertySources());
		environment.getPropertySources()
			.addFirst(new SystemEnvironmentPropertySource(DECRYPTED_PROPERTY_SOURCE_NAME, decrypted));
	}

}
