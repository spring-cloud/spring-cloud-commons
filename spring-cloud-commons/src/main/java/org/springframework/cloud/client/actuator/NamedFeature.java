package org.springframework.cloud.client.actuator;

import lombok.Value;

/**
 * @author Spencer Gibb
 */
@Value
public class NamedFeature {
	private final String name;
	private final Class<?> type;
}
