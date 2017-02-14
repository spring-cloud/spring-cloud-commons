package org.springframework.cloud.bootstrap;

import org.springframework.core.annotation.Order;

import static org.springframework.cloud.bootstrap.TestHigherPriorityBootstrapConfiguration.firstToBeCreated;

/**
 * @author Spencer Gibb
 */
@Order(0)
public class TestBootstrapConfiguration {

	public TestBootstrapConfiguration() {
		firstToBeCreated.compareAndSet(null, TestBootstrapConfiguration.class);
	}

}
