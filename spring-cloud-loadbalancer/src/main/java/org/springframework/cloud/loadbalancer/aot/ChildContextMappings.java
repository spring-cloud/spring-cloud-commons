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

package org.springframework.cloud.loadbalancer.aot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Olga Maciaszek-Sharma
 */
final class ChildContextMappings {

	private ChildContextMappings() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	static List<Entry> getChildContexts(ConfigurableApplicationContext applicationContext) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		String[] beanNames = applicationContext.getBeanNamesForType(LoadBalancerClientSpecification.class);
		List<Entry> contexts = new ArrayList<>();
		for (String beanName : beanNames) {
			contexts.add(process(beanFactory.getMergedBeanDefinition(beanName)));
		}
		return contexts;
	}

	private static Entry process(BeanDefinition beanDefinition) {
		ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
		ConstructorArgumentValues.ValueHolder nameValueHolder = constructorArguments.getIndexedArgumentValue(0, null);
		if (nameValueHolder != null) {
			String name = (String) nameValueHolder.getValue();
			ConstructorArgumentValues.ValueHolder configurationsValueHolder = constructorArguments
					.getIndexedArgumentValue(1, null);
			if (configurationsValueHolder != null) {
				String[] configurations = (String[]) configurationsValueHolder.getValue();
				return new Entry(name, configurations);
			}
		}
		throw new IllegalArgumentException("Invalid bean definition " + beanDefinition);
	}

	record Entry(String name, String[] configurations) {

	}

}
