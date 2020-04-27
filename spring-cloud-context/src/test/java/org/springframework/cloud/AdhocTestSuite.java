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

package org.springframework.cloud;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A test suite for probing weird ordering problems in the tests.
 *
 * @author Dave Syer
 */
@RunWith(Suite.class)
@SuiteClasses({ org.springframework.cloud.health.RefreshScopeHealthIndicatorTests.class,
		org.springframework.cloud.logging.LoggingRebinderTests.class,
		org.springframework.cloud.bootstrap.BootstrapSourcesOrderingTests.class,
		org.springframework.cloud.bootstrap.BootstrapDisabledAutoConfigurationIntegrationTests.class,
		org.springframework.cloud.bootstrap.BootstrapEnvironmentPostProcessorIntegrationTests.class,
		org.springframework.cloud.bootstrap.encrypt.RsaDisabledTests.class,
		org.springframework.cloud.bootstrap.encrypt.EncryptorFactoryTests.class,
		org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializerTests.class,
		org.springframework.cloud.bootstrap.encrypt.EncryptionBootstrapConfigurationTests.class,
		org.springframework.cloud.bootstrap.encrypt.EncryptionIntegrationTests.class,
		org.springframework.cloud.bootstrap.BootstrapOrderingSpringApplicationJsonIntegrationTests.class,
		org.springframework.cloud.bootstrap.config.BootstrapConfigurationTests.class,
		org.springframework.cloud.bootstrap.config.BootstrapListenerHierarchyIntegrationTests.class,
		org.springframework.cloud.bootstrap.BootstrapOrderingAutoConfigurationIntegrationTests.class,
		org.springframework.cloud.bootstrap.MessageSourceConfigurationTests.class,
		org.springframework.cloud.bootstrap.BootstrapOrderingCustomPropertySourceIntegrationTests.class,
		org.springframework.cloud.endpoint.RefreshEndpointTests.class,
		org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderIntegrationTests.class,
		org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderProxyIntegrationTests.class,
		org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderRefreshScopeIntegrationTests.class,
		org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderListIntegrationTests.class,
		org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderLifecycleIntegrationTests.class,
		org.springframework.cloud.context.named.NamedContextFactoryTests.class,
		org.springframework.cloud.context.refresh.ContextRefresherOrderingIntegrationTests.class,
		org.springframework.cloud.context.refresh.ContextRefresherIntegrationTests.class,
		org.springframework.cloud.context.refresh.ContextRefresherTests.class,
		org.springframework.cloud.context.environment.EnvironmentManagerTest.class,
		org.springframework.cloud.context.environment.EnvironmentManagerIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeNullBeanIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.MoreRefreshScopeIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeConcurrencyTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeScaleTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeLazyIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopePureScaleTests.class,
		org.springframework.cloud.context.scope.refresh.ImportRefreshScopeIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeSerializationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeListBindingIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeWebIntegrationTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationScaleTests.class,
		org.springframework.cloud.context.scope.refresh.RefreshEndpointIntegrationTests.class,
		org.springframework.cloud.context.restart.RestartIntegrationTests.class,
		org.springframework.cloud.autoconfigure.RefreshAutoConfigurationTests.class,
		org.springframework.cloud.autoconfigure.LifecycleMvcAutoConfigurationTests.class,
		org.springframework.cloud.autoconfigure.RefreshAutoConfigurationClassPathTests.class,
		org.springframework.cloud.autoconfigure.RefreshAutoConfigurationMoreClassPathTests.class })
@Ignore
public class AdhocTestSuite {

}
