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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerChildContextInitializer
		implements BeanRegistrationAotProcessor, BeanRegistrationExcludeFilter {

	private final ApplicationContext applicationContext;

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private final Map<String, ApplicationContextInitializer<ConfigurableApplicationContext>> applicationContextInitializers;

	public LoadBalancerChildContextInitializer(LoadBalancerClientFactory loadBalancerClientFactory,
			ApplicationContext applicationContext) {
		this(loadBalancerClientFactory, applicationContext, new HashMap<>());
	}

	public LoadBalancerChildContextInitializer(LoadBalancerClientFactory loadBalancerClientFactory,
			ApplicationContext applicationContext,
			Map<String, ApplicationContextInitializer<ConfigurableApplicationContext>> applicationContextInitializers) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.applicationContext = applicationContext;
		this.applicationContextInitializers = applicationContextInitializers;
	}

	private void registerBeans(ConfigurableApplicationContext childContext) {
		if (!applicationContextInitializers.isEmpty()) {
			applicationContextInitializers.keySet().stream()
					.filter(contextId -> contextId.equals(childContext.getDisplayName())).forEach(contextId -> {
						applicationContextInitializers.get(contextId).initialize(childContext);
						loadBalancerClientFactory.addContext(contextId, childContext);
					});
			return;
		}
		loadBalancerClientFactory.registerBeans(childContext.getDisplayName(),
				(AnnotationConfigApplicationContext) childContext);
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext);
		ConfigurableApplicationContext context = ((ConfigurableApplicationContext) applicationContext);
		BeanFactory applicationBeanFactory = context.getBeanFactory();
		if (!(registeredBean.getBeanClass().equals(getClass())
				&& registeredBean.getBeanFactory().equals(applicationBeanFactory))) {
			return null;
		}
		// TODO: add context ids from properties
		Set<String> contextIds = new HashSet<>();
		Map<String, LoadBalancerClientSpecification> configurations = loadBalancerClientFactory.getConfigurations();
		Set<String> contextIdsFromConfig = configurations.keySet().stream().filter(key -> !key.startsWith("default."))
				.collect(Collectors.toSet());
		contextIds.addAll(contextIdsFromConfig);
		contextIds.add("default");
		Map<String, ConfigurableApplicationContext> childContextAotContributions = contextIds.stream()
				.map(contextId -> Map.entry(contextId, buildChildContext(contextId)))
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		return new AotContribution(childContextAotContributions);
	}

	private ConfigurableApplicationContext buildChildContext(String contextId) {
		ConfigurableApplicationContext childContext = loadBalancerClientFactory.buildContext(contextId);
		registerBeans(childContext);
		return childContext;
	}

	@SuppressWarnings("unchecked")
	public LoadBalancerChildContextInitializer withApplicationContextInitializers(
			Map<String, Object> applicationContextInitializers) {
		Map<String, ApplicationContextInitializer<ConfigurableApplicationContext>> convertedInitializers = new HashMap<>();
		applicationContextInitializers.keySet()
				.forEach(contextId -> convertedInitializers.put(contextId,
						(ApplicationContextInitializer<ConfigurableApplicationContext>) applicationContextInitializers
								.get(contextId)));
		return new LoadBalancerChildContextInitializer(loadBalancerClientFactory, applicationContext,
				convertedInitializers);
	}

	@Override
	public boolean isExcluded(RegisteredBean registeredBean) {
		return false;
	}

	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Map<String, GenericApplicationContext> childContexts;

		AotContribution(Map<String, ConfigurableApplicationContext> childContexts) {
			this.childContexts = childContexts.entrySet().stream()
					.filter(entry -> entry.getValue() instanceof GenericApplicationContext)
					.map(entry -> Map.entry(entry.getKey(), (GenericApplicationContext) entry.getValue()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			ApplicationContextAotGenerator contextAotGenerator = new ApplicationContextAotGenerator();
			Map<String, ClassName> generatedInitializerClassNames = childContexts.entrySet().stream().map(entry -> {
				String name = entry.getValue().getDisplayName();
				name = name.replaceAll("[-]", "_");
				GenerationContext childGenerationContext = generationContext.withName(name);
				ClassName initializerClassName = contextAotGenerator.generateApplicationContext(entry.getValue(),
						childGenerationContext);
				return Map.entry(entry.getKey(), initializerClassName);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			GeneratedMethod postProcessorMethod = beanRegistrationCode.getMethodGenerator()
					.generateMethod("addChildContextInitializer").using(builder -> {
						builder.addJavadoc("Use AOT child context management initialization");
						builder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
						builder.addParameter(RegisteredBean.class, "registeredBean");
						builder.addParameter(LoadBalancerChildContextInitializer.class, "instance");
						builder.returns(LoadBalancerChildContextInitializer.class);
						builder.addStatement("$T<String, Object> initializers = new $T<>()", Map.class, HashMap.class);
						generatedInitializerClassNames.keySet()
								.forEach(contextId -> builder.addStatement("initializers.put($S, new $L())", contextId,
										generatedInitializerClassNames.get(contextId)));
						builder.addStatement("return instance.withApplicationContextInitializers(initializers)");
					});
			beanRegistrationCode.addInstancePostProcessor(
					MethodReference.ofStatic(beanRegistrationCode.getClassName(), postProcessorMethod.getName()));
			System.out.println("test");
		}

	}

}
