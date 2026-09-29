/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.context.named.fixture;

import org.springframework.cloud.context.named.NamedContextFactoryTests.TestType;
import org.springframework.context.annotation.Bean;

/**
 * A client-specific configuration for {@link NamedContextFactoryTests}. Deliberately has
 * the same simple name as the default configuration class
 * {@code org.springframework.cloud.context.named.DefaultClientConfiguration}. See
 * gh-1359.
 *
 * @author akenra
 */
public class DefaultClientConfiguration {

	@Bean
	public TestType testType() {
		return new TestType(1);
	}

}
