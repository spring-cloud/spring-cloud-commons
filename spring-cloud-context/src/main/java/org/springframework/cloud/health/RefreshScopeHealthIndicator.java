/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.health;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.scope.refresh.RefreshScope;

/**
 * Health indicator for the refresh scope and configuration properties rebinding. If an
 * environment change causes a bean to fail in instantiate or bind, this indicator will
 * generally say what the problem was and switch to DOWN.
 *
 * @author Dave Syer
 */
public class RefreshScopeHealthIndicator extends AbstractHealthIndicator {

	private ObjectProvider<RefreshScope> scope;

	private ConfigurationPropertiesRebinder rebinder;

	public RefreshScopeHealthIndicator(ObjectProvider<RefreshScope> scope,
			ConfigurationPropertiesRebinder rebinder) {
		this.scope = scope;
		this.rebinder = rebinder;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		RefreshScope refreshScope = this.scope.getIfAvailable();
		if (refreshScope != null) {
			Map<String, Exception> errors = new HashMap<>(refreshScope.getErrors());
			errors.putAll(this.rebinder.getErrors());
			if (errors.isEmpty()) {
				builder.up();
			}
			else {
				builder.down();
				if (errors.size() == 1) {
					builder.withException(errors.values().iterator().next());
				}
				else {
					for (String name : errors.keySet()) {
						builder.withDetail(name, errors.get(name));
					}
				}
			}
		}
	}

}
