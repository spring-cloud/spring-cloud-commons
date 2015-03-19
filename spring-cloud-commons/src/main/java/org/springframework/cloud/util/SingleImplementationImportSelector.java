/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.extern.apachecommons.CommonsLog;

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
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@CommonsLog
public abstract class SingleImplementationImportSelector<T> implements
		DeferredImportSelector, BeanClassLoaderAware, EnvironmentAware {

	private ClassLoader beanClassLoader;

	private Class<T> annotationClass;

	private Environment environment;

	@SuppressWarnings("unchecked")
	protected SingleImplementationImportSelector() {
		this.annotationClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(
				this.getClass(), SingleImplementationImportSelector.class);
	}

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		if (!isEnabled()) {
			return new String[0];
		}
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(this.annotationClass.getName(), true));

		Assert.notNull(attributes, "No " + getSimpleName() + " attributes found. Is "
				+ metadata.getClassName() + " annotated with @" + getSimpleName() + "?");

		// Find all possible auto configuration classes, filtering duplicates
		List<String> factories = new ArrayList<>(new LinkedHashSet<>(
				SpringFactoriesLoader.loadFactoryNames(this.annotationClass,
						this.beanClassLoader)));

		if (factories.isEmpty()) {
			throw new IllegalStateException("Annotation @" + getSimpleName() +
					" found, but there are no implementations.  Did you forget to include a starter?");
		}

		if (factories.size() > 1) {
			String factory = factories.get(0);
			// there should only every be one DiscoveryClient
			log.warn("More than one implementation " + "of @" + getSimpleName()
					+ ".  Using " + factory + " out of available " + factories);
			factories = Collections.singletonList(factory);
		}

		return factories.toArray(new String[factories.size()]);
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
