package org.springframework.cloud.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Taken from Spring Boot test utils.
 * https://github.com/spring-projects/spring-boot/blob/1.4.x/spring-boot/src/test/java/org/springframework/boot/testutil/ClassPathExclusions.java
 * @author Ryan Baxter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassPathExclusions {

	/**
	 * One or more Ant-style patterns that identify entries to be excluded from the class
	 * path. Matching is performed against an entry's {@link File#getName() file name}.
	 * For example, to exclude Hibernate Validator from the classpath,
	 * {@code "hibernate-validator-*.jar"} can be used.
	 * @return the exclusion patterns
	 */
	String[] value();
}
