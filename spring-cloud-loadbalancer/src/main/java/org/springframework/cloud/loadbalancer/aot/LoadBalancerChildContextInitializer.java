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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * A {@link BeanRegistrationAotProcessor} that creates an {@link AotContribution} for
 * LoadBalancer child contexts.
 *
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerChildContextInitializer
		implements BeanRegistrationAotProcessor, ApplicationListener<WebServerInitializedEvent> {

	private final ApplicationContext applicationContext;

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private final Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers;

	public LoadBalancerChildContextInitializer(LoadBalancerClientFactory loadBalancerClientFactory,
			ApplicationContext applicationContext) {
		this(loadBalancerClientFactory, applicationContext, new HashMap<>());
	}

	public LoadBalancerChildContextInitializer(LoadBalancerClientFactory loadBalancerClientFactory,
			ApplicationContext applicationContext,
			Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.applicationContext = applicationContext;
		this.applicationContextInitializers = applicationContextInitializers;
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
		Set<String> contextIds = new HashSet<>();
		contextIds.addAll(getContextIdsFromConfig());
		contextIds.addAll(getEagerLoadContextIds());
		Map<String, GenericApplicationContext> childContextAotContributions = contextIds.stream()
				.map(contextId -> Map.entry(contextId, buildChildContext(contextId)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return new AotContribution(childContextAotContributions);
	}

	private Set<String> getContextIdsFromConfig() {
		Map<String, LoadBalancerClientSpecification> configurations = loadBalancerClientFactory.getConfigurations();
		return configurations.keySet().stream().filter(key -> !key.startsWith("default.")).collect(Collectors.toSet());
	}

	private Set<String> getEagerLoadContextIds() {
		return Binder.get(applicationContext.getEnvironment())
				.bind("spring.cloud.loadbalancer.eager-load.clients", Bindable.setOf(String.class))
				.orElse(Collections.emptySet());
	}

	private GenericApplicationContext buildChildContext(String contextId) {
		GenericApplicationContext childContext = loadBalancerClientFactory.buildContext(contextId);
		loadBalancerClientFactory.registerBeans(contextId, childContext);
		return childContext;
	}

	@SuppressWarnings("unchecked")
	public LoadBalancerChildContextInitializer withApplicationContextInitializers(
			Map<String, Object> applicationContextInitializers) {
		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
		applicationContextInitializers.keySet()
				.forEach(contextId -> convertedInitializers.put(contextId,
						(ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers
								.get(contextId)));
		return new LoadBalancerChildContextInitializer(loadBalancerClientFactory, applicationContext,
				convertedInitializers);
	}

	@Override
	public boolean isBeanExcludedFromAotProcessing() {
		return false;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if (applicationContext.equals(event.getApplicationContext())) {
			applicationContextInitializers.keySet().forEach(contextId -> {
				GenericApplicationContext childContext = loadBalancerClientFactory.buildContext(contextId);
				applicationContextInitializers.get(contextId).initialize(childContext);
				loadBalancerClientFactory.addContext(contextId, childContext);
				childContext.refresh();
			});
		}
	}

	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Map<String, GenericApplicationContext> childContexts;

		AotContribution(Map<String, GenericApplicationContext> childContexts) {
			this.childContexts = childContexts.entrySet().stream().filter(entry -> entry.getValue() != null)
					.map(entry -> Map.entry(entry.getKey(), entry.getValue()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			Map<String, ClassName> generatedInitializerClassNames = childContexts.entrySet().stream().map(entry -> {
				String name = entry.getValue().getDisplayName();
				name = name.replaceAll("[-]", "_");
				GenerationContext childGenerationContext = generationContext.withName(name);
				ClassName initializerClassName = new ApplicationContextAotGenerator()
						.processAheadOfTime(entry.getValue(), childGenerationContext);
				return Map.entry(entry.getKey(), initializerClassName);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			GeneratedMethod postProcessorMethod = beanRegistrationCode.getMethods().add("addChildContextInitializer",
					method -> {
						method.addJavadoc("Use AOT child context management initialization")
								.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
								.addParameter(RegisteredBean.class, "registeredBean")
								.addParameter(LoadBalancerChildContextInitializer.class, "instance")
								.returns(LoadBalancerChildContextInitializer.class)
								.addStatement("$T<String, Object> initializers = new $T<>()", Map.class, HashMap.class);
						generatedInitializerClassNames.keySet()
								.forEach(contextId -> method.addStatement("initializers.put($S, new $L())", contextId,
										generatedInitializerClassNames.get(contextId)));
						method.addStatement("return instance.withApplicationContextInitializers(initializers)");
					});
			beanRegistrationCode.addInstancePostProcessor(postProcessorMethod.toMethodReference());
		}

	}

}
