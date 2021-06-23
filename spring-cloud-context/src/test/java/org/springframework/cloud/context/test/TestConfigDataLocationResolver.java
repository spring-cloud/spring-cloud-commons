/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.context.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Bindable;

public class TestConfigDataLocationResolver implements ConfigDataLocationResolver<TestConfigDataResource> {

	public static AtomicInteger count = new AtomicInteger(1);

	public static Map<String, Object> config = new HashMap<>();

	public static Object instance;

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix("testdatasource:");
	}

	@Override
	public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		if (instance != null) {
			BootstrapRegistry.InstanceSupplier<Object> supplier = BootstrapRegistry.InstanceSupplier.of(instance);
			Class<Object> aClass = (Class<Object>) instance.getClass();
			context.getBootstrapContext().registerIfAbsent(aClass, supplier);
		}
		String myplaceholder = context.getBinder().bind("myplaceholder", Bindable.of(String.class)).orElse("notfound");
		HashMap<String, Object> props = new HashMap<>(config);
		props.put(TestEnvPostProcessor.EPP_VALUE, count.get());
		if (count.get() == 99 && myplaceholder.contains("${vcap")) {
			throw new ConfigDataResourceNotFoundException(new TestConfigDataResource(props),
					new IllegalArgumentException("placeholder not resolved"));
		}
		return Collections.singletonList(new TestConfigDataResource(props));
	}

}
