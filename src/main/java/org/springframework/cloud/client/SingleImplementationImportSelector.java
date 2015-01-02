package org.springframework.cloud.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Selects a single configuration to load defined by the generic type T.
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Slf4j
public abstract class SingleImplementationImportSelector<T> implements
		DeferredImportSelector, BeanClassLoaderAware, EnvironmentAware {

	private ClassLoader beanClassLoader;

	private Class<T> annotationClass;
	private Environment environment;

	@SuppressWarnings("unchecked")
	protected SingleImplementationImportSelector() {
		annotationClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(
				this.getClass(), SingleImplementationImportSelector.class);
	}

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		if (!isEnabled()) {
			return new String[0];
		}
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(annotationClass.getName(), true));

		Assert.notNull(attributes, "No " + getSimpleName() + " attributes found. Is "
				+ metadata.getClassName() + " annotated with @" + getSimpleName() + "?");

		// Find all possible auto configuration classes, filtering duplicates
		List<String> factories = new ArrayList<>(new LinkedHashSet<>(
				SpringFactoriesLoader.loadFactoryNames(annotationClass,
						this.beanClassLoader)));

		if (factories.size() > 1) {
			String factory = factories.get(0);
			// there should only every be one DiscoveryClient
			log.warn(
					"More than one implementation of @{}.  Using {} out of available {}",
					getSimpleName(), factory, factories);
			factories = Collections.singletonList(factory);
		}

		return factories.toArray(new String[factories.size()]);

	}

	protected abstract boolean isEnabled();

	protected String getSimpleName() {
		return annotationClass.getSimpleName();
	}
	
	protected Class<T> getAnnotationClass() {
		return annotationClass;
	}

	protected Environment getEnvironment() {
		return environment;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}
}
