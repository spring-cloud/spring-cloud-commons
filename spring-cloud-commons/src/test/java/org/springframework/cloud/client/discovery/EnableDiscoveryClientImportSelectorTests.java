package org.springframework.cloud.client.discovery;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanClassLoader(getClass().getClassLoader());
		this.importSelector.setEnvironment(this.environment);
	}

	@Test
	public void autoRegistrationIsEnabled() {
		configureAnnotation(true);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertTrue(environment.getProperty("spring.cloud.service-registry.auto-registration.enabled", Boolean.class, true));
		assertThat(imports).hasSize(1);
	}

	@Test
	public void autoRegistrationIsDisabled() {
		configureAnnotation(false);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertFalse(environment.getProperty("spring.cloud.service-registry.auto-registration.enabled", Boolean.class));
		assertThat(imports).isEmpty();
	}

	private void configureAnnotation(boolean autoRegistration) {
		String annotationName = EnableDiscoveryClient.class.getName();
		given(this.annotationMetadata.isAnnotated(annotationName)).willReturn(true);
		given(this.annotationMetadata.getAnnotationAttributes(annotationName, true))
				.willReturn(this.annotationAttributes);
		given(this.annotationAttributes.getBoolean("autoRegister"))
				.willReturn(autoRegistration);
	}
}
