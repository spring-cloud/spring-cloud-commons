package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.List;

import lombok.Value;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties(prefix = "endpoints.features", ignoreUnknownFields = false)
public class FeaturesEndpoint extends AbstractEndpoint<FeaturesEndpoint.Features> implements ApplicationContextAware {

	private final List<HasFeatures> hasFeaturesList;
	private ApplicationContext context;

	public FeaturesEndpoint(List<HasFeatures> hasFeaturesList) {
		super("features", false);
		this.hasFeaturesList = hasFeaturesList;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@Override
	public Features invoke() {
		Features features = new Features();

		for (HasFeatures hasFeatures : hasFeaturesList) {
			List<Class> abstractFeatures = hasFeatures.getAbstractFeatures();
			if (abstractFeatures != null) {
				for (Class clazz : abstractFeatures) {
					addAbstractFeature(features, clazz);
				}
			}

			List<NamedFeature> namedFeatures = hasFeatures.getNamedFeatures();
			if (namedFeatures != null) {
				for (NamedFeature namedFeature : namedFeatures) {
					addFeature(features, namedFeature);
				}
			}
		}

		return features;
	}

	private void addAbstractFeature(Features features, Class type) {
		addFeature(features, new NamedFeature(type.getSimpleName(), type));
	}

	private void addFeature(Features features, NamedFeature feature) {
		try {
			Object bean = context.getBean(feature.getType());
			Class<?> beanClass = bean.getClass();
			features.getEnabled().add(new Feature(feature.getName(),
					beanClass.getCanonicalName(),
					beanClass.getPackage().getImplementationVersion(),
					beanClass.getPackage().getImplementationVendor()));
		} catch (NoSuchBeanDefinitionException e) {
			features.getDisabled().add(feature.getName());
		}
	}

	@Value
	class Features {
		List<Feature> enabled = new ArrayList<>();
		List<String> disabled = new ArrayList<>();
	}

	@Value
	class Feature {
		final String type;
		final String name;
		final String version;
		final String vendor;
	}
}
