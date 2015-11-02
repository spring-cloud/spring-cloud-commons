package org.springframework.cloud.client.actuator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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
		return new HasFeatures(Arrays.asList(abstractFeatures),
				Collections.<NamedFeature> emptyList());
	}

	public static HasFeatures namedFeatures(NamedFeature... namedFeatures) {
		return new HasFeatures(Collections.<Class> emptyList(),
				Arrays.asList(namedFeatures));
	}

	public static HasFeatures namedFeature(String name, Class<?> type) {
		return namedFeatures(new NamedFeature(name, type));
	}

	public static HasFeatures namedFeatures(String name1, Class<?> type1, String name2,
			Class<?> type2) {
		return namedFeatures(new NamedFeature(name1, type1),
				new NamedFeature(name2, type2));
	}
}
