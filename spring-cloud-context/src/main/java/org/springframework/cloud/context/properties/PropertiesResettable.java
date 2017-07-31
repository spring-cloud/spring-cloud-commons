package org.springframework.cloud.context.properties;

/**
 * To indicate that a {@link org.springframework.boot.context.properties.ConfigurationProperties} annotated bean
 * can fields can be reset - this is useful for cases where properties are removed from the environment
 */
public interface PropertiesResettable {

	/**
	 * Reset the fields 
	 */
	void reset();
}
