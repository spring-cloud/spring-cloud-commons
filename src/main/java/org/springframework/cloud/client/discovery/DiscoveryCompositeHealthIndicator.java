package org.springframework.cloud.client.discovery;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class DiscoveryCompositeHealthIndicator extends CompositeHealthIndicator {

	@Autowired
	public DiscoveryCompositeHealthIndicator(HealthAggregator healthAggregator, List<DiscoveryHealthIndicator> indicators) {
		super(healthAggregator);
		for (DiscoveryHealthIndicator indicator : indicators) {
			addHealthIndicator(indicator.getName(), new Holder(indicator));
		}
	}

	public static class Holder implements HealthIndicator {
		DiscoveryHealthIndicator delegate;

		public Holder(DiscoveryHealthIndicator delegate) {
			this.delegate = delegate;
		}

		@Override
		public Health health() {
			return delegate.health();
		}
	}
}
