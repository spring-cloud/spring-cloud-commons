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

package org.springframework.cloud.bootstrap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.style.ToStringCreator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

/**
 * This class uses {@link SpringFactoriesLoader} to load {@link BootstrapConfiguration}
 * entries from {@code spring.factories}. The classes are then loaded so they can be
 * sorted using {@link AnnotationAwareOrderComparator#sort(List)}. This class is a
 * {@link DeferredImportSelector} so {@code @Conditional} annotations on imported classes
 * are supported.
 *
 * @author Spencer Gibb
 */
public class BootstrapImportSelector implements EnvironmentAware, DeferredImportSelector {

	private Environment environment;

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String[] selectImports(AnnotationMetadata annotationMetadata) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// Use names and ensure unique to protect against duplicates
		List<String> names = new ArrayList<>(
				SpringFactoriesLoader.loadFactoryNames(BootstrapConfiguration.class, classLoader));
		names.addAll(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray(this.environment.getProperty("spring.cloud.bootstrap.sources", ""))));

		List<OrderedAnnotatedElement> elements = new ArrayList<>();
		for (String name : names) {
			try {
				elements.add(new OrderedAnnotatedElement(this.metadataReaderFactory, name));
			}
			catch (IOException e) {
				continue;
			}
		}
		AnnotationAwareOrderComparator.sort(elements);

		String[] classNames = elements.stream().map(e -> e.name).toArray(String[]::new);

		return classNames;
	}

	class OrderedAnnotatedElement implements AnnotatedElement {

		private final String name;

		private Order order = null;

		private Integer value;

		OrderedAnnotatedElement(MetadataReaderFactory metadataReaderFactory, String name) throws IOException {
			MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(name);
			AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
			Map<String, Object> attributes = metadata.getAnnotationAttributes(Order.class.getName());
			this.name = name;
			if (attributes != null && attributes.containsKey("value")) {
				this.value = (Integer) attributes.get("value");
				this.order = new Order() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return Order.class;
					}

					@Override
					public int value() {
						return OrderedAnnotatedElement.this.value;
					}
				};
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			if (annotationClass == Order.class) {
				return (T) this.order;
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return this.order == null ? new Annotation[0] : new Annotation[] { this.order };
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("name", this.name).append("value", this.value).toString();
		}

	}

}
