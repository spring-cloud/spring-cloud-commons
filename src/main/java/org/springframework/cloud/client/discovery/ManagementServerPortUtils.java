package org.springframework.cloud.client.discovery;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Spencer Gibb
 */
public class ManagementServerPortUtils {

	// TODO: copied from EndpointWebMvcAutoConfiguration.ManagementServerPort
	public static enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(BeanFactory beanFactory) {

			ServerProperties serverProperties;
			try {
				serverProperties = beanFactory.getBean(ServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				serverProperties = new ServerProperties();
			}

			ManagementServerProperties managementServerProperties;
			try {
				managementServerProperties = beanFactory
						.getBean(ManagementServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				managementServerProperties = new ManagementServerProperties();
			}

			Integer port = managementServerProperties.getPort();
			if (port != null && port < 0) {
				return DISABLE;
			}
			if (!(beanFactory instanceof WebApplicationContext)) {
				// Current context is not a webapp
				return DIFFERENT;
			}
			return ((port == null)
					|| (serverProperties.getPort() == null && port.equals(8080))
					|| (port != 0 && port.equals(serverProperties.getPort())) ? SAME
					: DIFFERENT);
		}
	};

	public static ManagementServerPort get(BeanFactory beanFactory) {
		return ManagementServerPort.get(beanFactory);
	}

	public static boolean isDifferent(BeanFactory beanFactory) {
		return get(beanFactory) == ManagementServerPort.DIFFERENT;
	}

	public static boolean isDisabled(BeanFactory beanFactory) {
		return get(beanFactory) == ManagementServerPort.DISABLE;
	}

	public static boolean isSame(BeanFactory beanFactory) {
		return get(beanFactory) == ManagementServerPort.SAME;
	}
}
