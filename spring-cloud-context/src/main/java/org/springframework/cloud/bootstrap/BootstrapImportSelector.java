/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * This class uses {@link SpringFactoriesLoader} to load {@link BootstrapConfiguration}
 * entries from {@code spring.factories}. The classes are then loaded so they can
 * be sorted using {@link AnnotationAwareOrderComparator#sort(List)}.
 * This class is a {@link DeferredImportSelector} so {@code @Conditional} annotations
 * on imported classes are supported.
 */
public class BootstrapImportSelector implements EnvironmentAware, DeferredImportSelector {

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// Use names and ensure unique to protect against duplicates
		List<String> names = new ArrayList<>(SpringFactoriesLoader
				.loadFactoryNames(BootstrapConfiguration.class, classLoader));
		names.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(
				environment.getProperty("spring.cloud.bootstrap.sources", ""))));

		List<Class<?>> sources = new ArrayList<>();
		for (String name : names) {
			Class<?> cls = ClassUtils.resolveClassName(name, null);
			try {
				cls.getDeclaredAnnotations();
			}
			catch (Exception e) {
				continue;
			}
			sources.add(cls);
		}
		AnnotationAwareOrderComparator.sort(sources);

		List<String> classNames = sources.stream()
				.map(Class::getName)
				.collect(Collectors.toList());


		return classNames.toArray(new String[0]);
	}
}
