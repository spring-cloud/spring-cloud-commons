package org.springframework.cloud.client.serviceregistry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.client.discovery.ManagementServerPortUtils;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lifecycle methods that may be useful and common to {@link ServiceRegistry} implementations.
 *
 * TODO: document the lifecycle
 *
 * @param <R> registration type passed to the {@link ServiceRegistry}.
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAutoServiceRegistration<R extends Registration> implements AutoServiceRegistration, ApplicationContextAware {
	private static final Log logger = LogFactory.getLog(AbstractAutoServiceRegistration.class);

	private boolean autoStartup = true;

	private AtomicBoolean running = new AtomicBoolean(false);

	private int order = 0;

	private ApplicationContext context;

	private Environment environment;

	private AtomicInteger port = new AtomicInteger(0);

	protected ApplicationContext getContext() {
		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
		this.environment = this.context.getEnvironment();
	}

	@Deprecated
	protected Environment getEnvironment() {
		return environment;
	}

	@Deprecated
	protected AtomicInteger getPort() {
		return port;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void start() {
		if (!isEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Discovery Lifecycle disabled. Not starting");
			}
			return;
		}

		/*// only set the port if the nonSecurePort is 0 and this.port != 0
		if (this.port.get() != 0 && getConfiguredPort() == 0) {
			setConfiguredPort(this.port.get());
		}
		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!this.running.get() && getConfiguredPort() > 0) {
			register();
			if (shouldRegisterManagement()) {
				registerManagement();
			}
			this.context.publishEvent(new InstanceRegisteredEvent<>(this,
					getConfiguration()));
			this.running.compareAndSet(false, true);
		}*/
	}

	/**
	 * @return if the management service should be registered with the {@link ServiceRegistry}
	 */
	protected boolean shouldRegisterManagement() {
		return getManagementPort() != null && ManagementServerPortUtils.isDifferent(this.context);
	}

	/**
	 * @return the object used to configure the registration
	 */
	@Deprecated
	protected abstract Object getConfiguration();


	/**
	 * @return true, if this is enabled
	 */
	protected abstract boolean isEnabled();

	/**
	 * @return the serviceId of the Management Service
	 */
	@Deprecated
	protected String getManagementServiceId() {
		// TODO: configurable management suffix
		return this.context.getId() + ":management";
	}

	/**
	 * @return the service name of the Management Service
	 */
	@Deprecated
	protected String getManagementServiceName() {
		// TODO: configurable management suffix
		return getAppName() + ":management";
	}

	/**
	 * @return the management server port
	 */
	@Deprecated
	protected Integer getManagementPort() {
		return ManagementServerPortUtils.getPort(this.context);
	}

	/**
	 * @return the app name, currently the spring.application.name property
	 */
	@Deprecated
	protected String getAppName() {
		return this.environment.getProperty("spring.application.name", "application");
	}
	@PreDestroy
	public void destroy() {
		stop();
	}

	public boolean isRunning() {
		return this.running.get();
	}

	protected AtomicBoolean getRunning() {
		return running;
	}

	public int getOrder() {
		return this.order;
	}

	public int getPhase() {
		return 0;
	}

	/*@Deprecated
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		// TODO: take SSL into account
		// Don't register the management port as THE port
		if (!"management".equals(event.getApplicationContext().getNamespace())) {
			this.port.compareAndSet(0, event.getEmbeddedServletContainer().getPort());
			this.start();
		}
	}*/

	private ServiceRegistry<R> serviceRegistry;

	protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected ServiceRegistry<R> getServiceRegistry() {
		return this.serviceRegistry;
	}

	protected abstract R getRegistration();

	protected abstract R getManagementRegistration();

	/**
	 * Register the local service with the {@link ServiceRegistry}
	 */
	protected void register() {
		this.serviceRegistry.register(getRegistration());
	}

	/**
	 * Register the local management service with the {@link ServiceRegistry}
	 */
	protected void registerManagement() {
		this.serviceRegistry.register(getManagementRegistration());
	}

	/**
	 * De-register the local service with the {@link ServiceRegistry}
	 */
	protected void deregister() {
		this.serviceRegistry.deregister(getRegistration());
	}

	/**
	 * De-register the local management service with the {@link ServiceRegistry}
	 */
	protected void deregisterManagement() {
		this.serviceRegistry.deregister(getManagementRegistration());
	}

	public void stop() {
		if (this.getRunning().compareAndSet(true, false) && isEnabled()) {
			deregister();
			if (shouldRegisterManagement()) {
				deregisterManagement();
			}
			this.serviceRegistry.close();
		}
	}

}
