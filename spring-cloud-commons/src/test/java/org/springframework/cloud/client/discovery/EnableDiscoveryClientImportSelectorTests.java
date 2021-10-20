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

package org.springframework.cloud.client.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

/**
 * @author Spencer Gibb
 */
public class EnableDiscoveryClientImportSelectorTests {

	private final EnableDiscoveryClientImportSelector importSelector = new EnableDiscoveryClientImportSelector();

	private final MockEnvironment environment = new MockEnvironment();

	@Mock
	private AnnotationMetadata annotationMetadata;

	@Mock
	private AnnotationAttributes annotationAttributes;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanClassLoader(getClass().getClassLoader());
		this.importSelector.setEnvironment(this.environment);
	}

	@Test
	public void autoRegistrationIsEnabled() {
		configureAnnotation(true);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		then(this.environment.getProperty("spring.cloud.service-registry.auto-registration.enabled", Boolean.class,
				true)).isTrue();
		then(imports).hasSize(1);
	}

	@Test
	public void autoRegistrationIsDisabled() {
		configureAnnotation(false);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		then(this.environment.getProperty("spring.cloud.service-registry.auto-registration.enabled", Boolean.class))
				.isFalse();
		then(imports).isEmpty();
	}

	private void configureAnnotation(boolean autoRegistration) {
		String annotationName = EnableDiscoveryClient.class.getName();
		given(this.annotationMetadata.isAnnotated(annotationName)).willReturn(true);
		given(this.annotationMetadata.getAnnotationAttributes(annotationName, true))
				.willReturn(this.annotationAttributes);
		given(this.annotationAttributes.getBoolean("autoRegister")).willReturn(autoRegistration);
	}

}
