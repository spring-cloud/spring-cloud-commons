package org.springframework.cloud.bootstrap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestHigherPriorityBootstrapConfiguration {

	static final AtomicReference<Class<?>> firstToBeCreated = new AtomicReference<>();
	
	public static final AtomicInteger count = new AtomicInteger();

	public TestHigherPriorityBootstrapConfiguration() {
		count.incrementAndGet();
		firstToBeCreated.compareAndSet(null, TestHigherPriorityBootstrapConfiguration.class);
	}

}
