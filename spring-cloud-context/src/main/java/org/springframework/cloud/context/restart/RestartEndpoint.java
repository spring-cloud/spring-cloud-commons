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

package org.springframework.cloud.context.restart;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.util.ClassUtils;

/**
 * An endpoint that restarts the application context. Install as a bean and also register
 * a {@link RestartListener} with the {@link SpringApplication} that starts the context.
 * Those two components communicate via an {@link ApplicationEvent} and set up the state
 * needed to doRestart the context.
 *
 * @author Dave Syer
 *
 */
@Endpoint(id = "restart", enableByDefault = false)
public class RestartEndpoint implements ApplicationListener<ApplicationPreparedEvent> {

	private static Log logger = LogFactory.getLog(RestartEndpoint.class);

	private ConfigurableApplicationContext context;

	private SpringApplication application;

	private String[] args;

	private ApplicationPreparedEvent event;

	private IntegrationShutdown integrationShutdown;

	private List<PauseHandler> pauseHandlers = Collections.emptyList();

	private long timeout;

	// @ManagedAttribute
	public long getTimeout() {
		return this.timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setIntegrationMBeanExporter(Object exporter) {
		if (exporter != null) {
			this.integrationShutdown = new IntegrationShutdown(exporter);
		}
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent input) {
		this.event = input;
		if (this.context == null) {
			this.context = this.event.getApplicationContext();
			this.args = this.event.getArgs();
			this.application = this.event.getSpringApplication();
			this.application.addInitializers(new PostProcessorInitializer());
			this.pauseHandlers = this.context.getBeanProvider(PauseHandler.class).orderedStream()
					.collect(Collectors.toList());
		}
	}

	@WriteOperation
	public Object restart() {
		Thread thread = new Thread(this::safeRestart);
		thread.setDaemon(false);
		thread.start();
		return Collections.singletonMap("message", "Restarting");
	}

	private Boolean safeRestart() {
		try {
			doRestart();
			logger.info("Restarted");
			return true;
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.info("Could not doRestart", e);
			}
			else {
				logger.info("Could not doRestart: " + e.getMessage());
			}
			return false;
		}
	}

	public PauseEndpoint getPauseEndpoint() {
		return new PauseEndpoint();
	}

	public ResumeEndpoint getResumeEndpoint() {
		return new ResumeEndpoint();
	}

	// @ManagedOperation
	public synchronized ConfigurableApplicationContext doRestart() {
		if (this.context != null) {
			if (this.integrationShutdown != null) {
				this.integrationShutdown.stop(this.timeout);
			}
			this.application.setEnvironment(this.context.getEnvironment());
			close();
			// If running in a webapp then the context classloader is probably going to
			// die so we need to revert to a safe place before starting again
			overrideClassLoaderForRestart();
			this.context = this.application.run(this.args);
		}
		return this.context;
	}

	private void close() {
		ApplicationContext context = this.context;
		while (context instanceof Closeable) {
			try {
				((Closeable) context).close();
			}
			catch (IOException e) {
				logger.error("Cannot close context: " + context.getId(), e);
			}
			context = context.getParent();
		}
	}

	// @ManagedAttribute
	public boolean isRunning() {
		if (this.context != null) {
			return this.context.isRunning();
		}
		return false;
	}

	// @ManagedOperation
	public synchronized void doPause() {
		for (PauseHandler handler : this.pauseHandlers) {
			handler.pause();
		}
	}

	// @ManagedOperation
	public synchronized void doResume() {
		for (int i = this.pauseHandlers.size(); i-- > 0;) {
			PauseHandler handler = this.pauseHandlers.get(i);
			handler.resume();
		}
	}

	private void overrideClassLoaderForRestart() {
		ClassUtils.overrideThreadContextClassLoader(this.application.getClass().getClassLoader());
	}

	class PostProcessorInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext context) {
			context.registerBean(PostProcessor.class, () -> new PostProcessor());
		}

	}

	class PostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof RestartEndpoint) {
				return RestartEndpoint.this;
			}
			return bean;
		}

	}

	/**
	 * Pause endpoint configuration.
	 */
	@Endpoint(id = "pause")
	public class PauseEndpoint {

		@WriteOperation
		public Boolean pause() {
			if (isRunning()) {
				doPause();
				return true;
			}
			return false;
		}

	}

	/**
	 * Resume endpoint configuration.
	 */
	@Endpoint(id = "resume")
	@ConfigurationProperties("management.endpoint.resume")
	public class ResumeEndpoint {

		@WriteOperation
		public Boolean resume() {
			if (!isRunning()) {
				doResume();
				return true;
			}
			return false;
		}

	}

	private class IntegrationShutdown {

		private IntegrationMBeanExporter exporter;

		IntegrationShutdown(Object exporter) {
			this.exporter = (IntegrationMBeanExporter) exporter;
		}

		public void stop(long timeout) {
			this.exporter.stopActiveComponents(timeout);
		}

	}

}
