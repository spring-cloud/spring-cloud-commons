package org.springframework.cloud.client.discovery;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * @author Spencer Gibb
 */
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
