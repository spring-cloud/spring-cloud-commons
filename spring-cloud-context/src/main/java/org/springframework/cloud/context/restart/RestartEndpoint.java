/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.context.restart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.ClassUtils;

/**
 * An endpoint that restarts the application context. Install as a bean and also register
 * a {@link RestartListener} with the {@link SpringApplication} that starts the context.
 * Those two components communicate via an {@link ApplicationEvent} and set up the state
 * needed to restart the context.
 *
 * @author Dave Syer
 *
 */
@ConfigurationProperties("endpoints.restart")
@ManagedResource
public class RestartEndpoint extends AbstractEndpoint<Boolean>
		implements ApplicationListener<ApplicationPreparedEvent> {

	private static Log logger = LogFactory.getLog(RestartEndpoint.class);

	public RestartEndpoint() {
		super("restart", true, false);
	}

	private ConfigurableApplicationContext context;

	private SpringApplication application;

	private String[] args;

	private ApplicationPreparedEvent event;

	private IntegrationShutdown integrationShutdown;

	private long timeout;

	@ManagedAttribute
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
		}
	}

	@Override
	public Boolean invoke() {
		try {
			restart();
			logger.info("Restarted");
			return true;
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.info("Could not restart", e);
			}
			else {
				logger.info("Could not restart: " + e.getMessage());
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

	@ConfigurationProperties("endpoints.pause")
	public class PauseEndpoint extends AbstractEndpoint<Boolean> {

		public PauseEndpoint() {
			super("pause", true);
		}

		@Override
		public Boolean invoke() {
			if (isRunning()) {
				pause();
				return true;
			}
			return false;
		}
	}

	@ConfigurationProperties("endpoints.resume")
	public class ResumeEndpoint extends AbstractEndpoint<Boolean> {

		public ResumeEndpoint() {
			super("resume", true);
		}

		@Override
		public Boolean invoke() {
			if (!isRunning()) {
				resume();
				return true;
			}
			return false;
		}
	}

	@ManagedOperation
	public synchronized ConfigurableApplicationContext restart() {
		if (this.context != null) {
			if (this.integrationShutdown != null) {
				this.integrationShutdown.stop(this.timeout);
			}
			this.application.setEnvironment(this.context.getEnvironment());
			this.context.close();
			// If running in a webapp then the context classloader is probably going to
			// die so we need to revert to a safe place before starting again
			overrideClassLoaderForRestart();
			this.context = this.application.run(this.args);
		}
		return this.context;
	}

	@ManagedAttribute
	public boolean isRunning() {
		if (this.context != null) {
			return this.context.isRunning();
		}
		return false;
	}

	@ManagedOperation
	public synchronized void pause() {
		if (this.context != null) {
			this.context.stop();
		}
	}

	@ManagedOperation
	public synchronized void resume() {
		if (this.context != null) {
			this.context.start();
		}
	}

	private void overrideClassLoaderForRestart() {
		ClassUtils.overrideThreadContextClassLoader(
				this.application.getClass().getClassLoader());
	}

	private class IntegrationShutdown {

		private IntegrationMBeanExporter exporter;

		public IntegrationShutdown(Object exporter) {
			this.exporter = (IntegrationMBeanExporter) exporter;
		}

		public void stop(long timeout) {
			this.exporter.stopActiveComponents(timeout);
		}
	}

}
