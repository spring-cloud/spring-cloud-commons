package org.springframework.cloud.client.circuitbreaker;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(EnableCircuitBreakerImportSelector.class)
public @interface EnableCircuitBreaker {
}
