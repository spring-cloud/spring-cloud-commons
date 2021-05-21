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

package org.springframework.cloud.bootstrap;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;

/**
 * BootstrapRegistryInitializer that adds the BootstrapContext to the ApplicationContext
 * for use later in {@link ConfigDataContextRefresher}.
 *
 * @author Spencer Gibb
 * @since 3.0.3
 */
public class RefreshBootstrapRegistryInitializer implements BootstrapRegistryInitializer {

	@Override
	public void initialize(BootstrapRegistry registry) {
		// promote BootstrapContext to context
		registry.addCloseListener(event -> {
			BootstrapContext bootstrapContext = event.getBootstrapContext();
			event.getApplicationContext().getBeanFactory().registerSingleton("bootstrapContext", bootstrapContext);
		});
	}

}
