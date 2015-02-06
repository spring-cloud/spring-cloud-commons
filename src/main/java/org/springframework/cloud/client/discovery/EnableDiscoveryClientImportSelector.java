/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.cloud.util.SingleImplementationImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EnableDiscoveryClientImportSelector extends
		SingleImplementationImportSelector<EnableDiscoveryClient> {

	@Override
	protected boolean isEnabled() {
		return new RelaxedPropertyResolver(getEnvironment()).getProperty(
				"spring.cloud.discovery.enabled", Boolean.class, Boolean.TRUE);
	}

}
