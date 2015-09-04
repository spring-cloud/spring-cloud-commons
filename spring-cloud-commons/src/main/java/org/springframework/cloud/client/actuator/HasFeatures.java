package org.springframework.cloud.client.actuator;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Value
@Builder
public class HasFeatures {
	@Singular
	private final List<Class> abstractFeatures;
	@Singular
	private final List<NamedFeature> namedFeatures;

	public static HasFeatures abstractFeatures(Class... abstractFeatures) {
		return new HasFeatures(Arrays.asList(abstractFeatures), Collections.<NamedFeature>emptyList());
	}

	public static HasFeatures namedFeatures(NamedFeature... namedFeatures) {
		return new HasFeatures(Collections.<Class>emptyList(), Arrays.asList(namedFeatures));
	}
}
