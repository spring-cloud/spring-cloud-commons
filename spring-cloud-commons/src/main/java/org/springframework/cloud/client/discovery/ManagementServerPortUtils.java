/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.client.discovery;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Spencer Gibb
 */
public class ManagementServerPortUtils {
	private static final boolean hasActuator;
	static {
		boolean hasClass;
		try {
			Class.forName("org.springframework.boot.actuate.autoconfigure.ManagementServerProperties");
			hasClass = true;
		} catch (ClassNotFoundException e) {
			hasClass = false;
		}
		hasActuator = hasClass;
	}

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

	public static Integer getPort(BeanFactory beanFactory) {
		if (!hasActuator) {
			return null;
		}
		try {
			ManagementServerProperties properties = beanFactory
					.getBean(ManagementServerProperties.class);
			return properties.getPort();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	// TODO: copied from EndpointWebMvcAutoConfiguration.ManagementServerPort
	public static enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(BeanFactory beanFactory) {
			if (!hasActuator) {
				return SAME;
			}

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
	}

}
