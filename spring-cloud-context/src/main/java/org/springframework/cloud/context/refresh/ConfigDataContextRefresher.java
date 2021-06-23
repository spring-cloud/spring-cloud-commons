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

package org.springframework.cloud.context.refresh;

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.util.Instantiator;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * @author Dave Syer
 * @author Venil Noronha
 */
public class ConfigDataContextRefresher extends ContextRefresher
		implements ApplicationListener<ApplicationPreparedEvent> {

	private SpringApplication application;

	@Deprecated
	public ConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
		super(context, scope);
	}

	public ConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
			RefreshAutoConfiguration.RefreshProperties properties) {
		super(context, scope, properties);
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		application = event.getSpringApplication();
	}

	@Override
	protected void updateEnvironment() {
		if (logger.isTraceEnabled()) {
			logger.trace("Re-processing environment to add config data");
		}
		StandardEnvironment environment = copyEnvironment(getContext().getEnvironment());
		ConfigurableBootstrapContext bootstrapContext = getContext().getBeanProvider(ConfigurableBootstrapContext.class)
				.getIfAvailable(DefaultBootstrapContext::new);

		// run thru all EnvironmentPostProcessor instances. This lets things like vcap and
		// decrypt happen after refresh. The hard coded call to
		// ConfigDataEnvironmentPostProcessor.applyTo() is now automated as well.
		DeferredLogFactory logFactory = new PassthruDeferredLogFactory();
		List<String> classNames = SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
				getClass().getClassLoader());
		Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
				(parameters) -> {
					parameters.add(DeferredLogFactory.class, logFactory);
					parameters.add(Log.class, logFactory::getLog);
					parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
					parameters.add(BootstrapContext.class, bootstrapContext);
					parameters.add(BootstrapRegistry.class, bootstrapContext);
				});
		List<EnvironmentPostProcessor> postProcessors = instantiator.instantiate(classNames);
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(environment, application);
		}

		if (environment.getPropertySources().contains(REFRESH_ARGS_PROPERTY_SOURCE)) {
			environment.getPropertySources().remove(REFRESH_ARGS_PROPERTY_SOURCE);
		}
		MutablePropertySources target = getContext().getEnvironment().getPropertySources();
		String targetName = null;
		for (PropertySource<?> source : environment.getPropertySources()) {
			String name = source.getName();
			if (target.contains(name)) {
				targetName = name;
			}
			if (!this.standardSources.contains(name)) {
				if (target.contains(name)) {
					target.replace(name, source);
				}
				else {
					if (targetName != null) {
						target.addAfter(targetName, source);
						// update targetName to preserve ordering
						targetName = name;
					}
					else {
						// targetName was null so we are at the start of the list
						target.addFirst(source);
						targetName = name;
					}
				}
			}
		}
	}

	static class PassthruDeferredLogFactory implements DeferredLogFactory {

		@Override
		public Log getLog(Supplier<Log> destination) {
			return destination.get();
		}

		@Override
		public Log getLog(Class<?> destination) {
			return getLog(() -> LogFactory.getLog(destination));
		}

		@Override
		public Log getLog(Log destination) {
			return getLog(() -> destination);
		}

	}

}
