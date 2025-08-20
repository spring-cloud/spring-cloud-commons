/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.client.circuitbreaker.httpservice;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRestClientHttpServiceGroupConfigurerTests.GROUP_NAME;

/**
 * @author Olga Maciaszek-Sharma
 */
// Group specified, no services specified, "default" config key
@HttpServiceFallback(value = EmptyFallbacks.class, group = GROUP_NAME)
// Group specified, service specified, service name key
@HttpServiceFallback(value = Fallbacks.class, service = TestService.class, group = GROUP_NAME)
// Ignored as overridden by per-group configuration
@HttpServiceFallback(value = EmptyFallbacks.class, service = TestService.class)
class FallbacksPerGroupConfiguration {

}
