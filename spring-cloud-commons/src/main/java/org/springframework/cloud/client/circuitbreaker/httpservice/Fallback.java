package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * @author Olga Maciaszek-Sharma
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fallback {

	@AliasFor("forService")
	String value() default "";

	@AliasFor("value")
	Class<?>[] forService() default {};

	@AliasFor
	String forGroup() default "";
}
