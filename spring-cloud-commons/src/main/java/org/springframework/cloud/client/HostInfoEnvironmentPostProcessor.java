package org.springframework.cloud.client;

import java.util.LinkedHashMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.util.InetUtils;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Spencer Gibb
 */
public class HostInfoEnvironmentPostProcessor implements
		EnvironmentPostProcessor, Ordered {

	// Before ConfigFileApplicationListener
	private int order = ConfigFileEnvironmentPostProcessor.DEFAULT_ORDER - 1;

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		InetUtils.HostInfo hostInfo = InetUtils.getFirstNonLoopbackHostInfo();
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("spring.cloud.client.hostname", hostInfo.getHostname());
		map.put("spring.cloud.client.ipAddress", hostInfo.getIpAddress());
		MapPropertySource propertySource = new MapPropertySource("springCloudClientHostInfo", map);
		environment.getPropertySources().addLast(propertySource);
	}
}
