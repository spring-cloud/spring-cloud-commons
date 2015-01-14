package org.springframework.cloud.client.discovery;

import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * @author Spencer Gibb
 */
public abstract class AbstractDiscoveryLifecycle implements DiscoveryLifecycle,
		ApplicationContextAware {

	protected boolean autoStartup = true;
	protected boolean running;
	protected int order = 0;
	protected ApplicationContext context;
	protected Environment environment;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
		this.environment = this.context.getEnvironment();
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		if (!isEnabled()) {
			return;
		}

		register();
		if (ManagementServerPortUtils.isDifferent(this.context)) {
			registerManagement();
		}
		this.context
				.publishEvent(new InstanceRegisteredEvent<>(this, getConfiguration()));
		this.running = true;
	}

	protected abstract Object getConfiguration();

	protected abstract void register();

	protected void registerManagement() {
	}

	protected abstract void deregister();

	protected void deregisterManagement() {
	}

	protected abstract boolean isEnabled();

	protected String getManagementServiceId() {
		return this.context.getId() + ":management"; // TODO: configurable management
														// suffix
	}

	protected String getManagementServiceName() {
		return getAppName() + ":management"; // TODO: configurable management suffix
	}

	protected Integer getManagementPort() {
		return this.context.getBean(ManagementServerProperties.class).getPort();
	}

	protected String getAppName() {
		return this.environment.getProperty("spring.application.name");
	}

	@Override
	public void stop() {
		if (isEnabled()) {
			deregister();
			if (getManagementPort() != null) {
				deregisterManagement();
			}
		}
		this.running = false;
	}

	@PreDestroy
	public void destroy() {
		stop();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public int getPhase() {
		return 0;
	}
}
