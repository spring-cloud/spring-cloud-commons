/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.cloud.client.discovery.simple;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientConfigurations.StandardSimpleDiscoveryClientConfiguration;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientConfigurations.WebApplicationSimpleDiscoveryClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration for simple properties-based discovery client.
 *
 * @author Biju Kunjummen
 * @author Charu Covindane
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CommonsClientAutoConfiguration.class })
@Import({ StandardSimpleDiscoveryClientConfiguration.class, WebApplicationSimpleDiscoveryClientConfiguration.class })
public class SimpleDiscoveryClientAutoConfiguration {

}
