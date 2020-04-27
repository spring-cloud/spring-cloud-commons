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

package org.springframework.cloud.commons.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * Selects configurations to load, defined by the generic type T. Loads implementations
 * using {@link SpringFactoriesLoader}.
 *
 * @param <T> type of annotation class
 * @author Spencer Gibb
 * @author Dave Syer
 */
public abstract class SpringFactoryImportSelector<T>
		implements DeferredImportSelector, BeanClassLoaderAware, EnvironmentAware {

	private final Log log = LogFactory.getLog(SpringFactoryImportSelector.class);

	private ClassLoader beanClassLoader;

	private Class<T> annotationClass;

	private Environment environment;

	@SuppressWarnings("unchecked")
	protected SpringFactoryImportSelector() {
		this.annotationClass = (Class<T>) GenericTypeResolver
				.resolveTypeArgument(this.getClass(), SpringFactoryImportSelector.class);
	}

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		if (!isEnabled()) {
			return new String[0];
		}
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(
				metadata.getAnnotationAttributes(this.annotationClass.getName(), true));

		Assert.notNull(attributes, "No " + getSimpleName() + " attributes found. Is "
				+ metadata.getClassName() + " annotated with @" + getSimpleName() + "?");

		// Find all possible auto configuration classes, filtering duplicates
		List<String> factories = new ArrayList<>(new LinkedHashSet<>(SpringFactoriesLoader
				.loadFactoryNames(this.annotationClass, this.beanClassLoader)));

		if (factories.isEmpty() && !hasDefaultFactory()) {
			throw new IllegalStateException("Annotation @" + getSimpleName()
					+ " found, but there are no implementations. Did you forget to include a starter?");
		}

		if (factories.size() > 1) {
			// there should only ever be one DiscoveryClient, but there might be more than
			// one factory
			this.log.warn("More than one implementation " + "of @" + getSimpleName()
					+ " (now relying on @Conditionals to pick one): " + factories);
		}

		return factories.toArray(new String[factories.size()]);
	}

	protected boolean hasDefaultFactory() {
		return false;
	}

	protected abstract boolean isEnabled();

	protected String getSimpleName() {
		return this.annotationClass.getSimpleName();
	}

	protected Class<T> getAnnotationClass() {
		return this.annotationClass;
	}

	protected Environment getEnvironment() {
		return this.environment;
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
