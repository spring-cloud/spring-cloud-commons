/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.cloud;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.springframework.cloud.autoconfigure.LifecycleMvcAutoConfigurationTests;
import org.springframework.cloud.autoconfigure.RefreshAutoConfigurationClassPathTests;
import org.springframework.cloud.autoconfigure.RefreshAutoConfigurationMoreClassPathTests;
import org.springframework.cloud.autoconfigure.RefreshAutoConfigurationTests;
import org.springframework.cloud.bootstrap.BootstrapDisabledAutoConfigurationIntegrationTests;
import org.springframework.cloud.bootstrap.BootstrapOrderingAutoConfigurationIntegrationTests;
import org.springframework.cloud.bootstrap.BootstrapOrderingCustomPropertySourceIntegrationTests;
import org.springframework.cloud.bootstrap.BootstrapOrderingSpringApplicationJsonIntegrationTests;
import org.springframework.cloud.bootstrap.BootstrapSourcesOrderingTests;
import org.springframework.cloud.bootstrap.MessageSourceConfigurationTests;
import org.springframework.cloud.bootstrap.config.BootstrapConfigurationTests;
import org.springframework.cloud.bootstrap.config.BootstrapListenerHierarchyIntegrationTests;
import org.springframework.cloud.bootstrap.encrypt.EncryptionBootstrapConfigurationTests;
import org.springframework.cloud.bootstrap.encrypt.EncryptionIntegrationTests;
import org.springframework.cloud.bootstrap.encrypt.EncryptorFactoryTests;
import org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializerTests;
import org.springframework.cloud.bootstrap.encrypt.RsaDisabledTests;
import org.springframework.cloud.context.environment.EnvironmentManagerIntegrationTests;
import org.springframework.cloud.context.environment.EnvironmentManagerTest;
import org.springframework.cloud.context.named.NamedContextFactoryTests;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderIntegrationTests;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderLifecycleIntegrationTests;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderListIntegrationTests;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderProxyIntegrationTests;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinderRefreshScopeIntegrationTests;
import org.springframework.cloud.context.refresh.ContextRefresherIntegrationTests;
import org.springframework.cloud.context.refresh.ContextRefresherTests;
import org.springframework.cloud.context.restart.RestartIntegrationTests;
import org.springframework.cloud.context.scope.refresh.ImportRefreshScopeIntegrationTests;
import org.springframework.cloud.context.scope.refresh.MoreRefreshScopeIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshEndpointIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeConcurrencyTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationScaleTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeConfigurationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeLazyIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeListBindingIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeNullBeanIntegrationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopePureScaleTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeScaleTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeSerializationTests;
import org.springframework.cloud.context.scope.refresh.RefreshScopeWebIntegrationTests;
import org.springframework.cloud.endpoint.RefreshEndpointTests;
import org.springframework.cloud.health.RefreshScopeHealthIndicatorTests;
import org.springframework.cloud.logging.LoggingRebinderTests;

/**
 * A test suite for probing weird ordering problems in the tests.
 *
 * @author Dave Syer
 */
@RunWith(Suite.class)
@SuiteClasses({ BootstrapSourcesOrderingTests.class,
		BootstrapDisabledAutoConfigurationIntegrationTests.class,
		EncryptionBootstrapConfigurationTests.class, RsaDisabledTests.class,
		EncryptorFactoryTests.class, EnvironmentDecryptApplicationInitializerTests.class,
		EncryptionIntegrationTests.class,
		BootstrapOrderingAutoConfigurationIntegrationTests.class,
		MessageSourceConfigurationTests.class,
		BootstrapOrderingSpringApplicationJsonIntegrationTests.class,
		BootstrapConfigurationTests.class,
		BootstrapListenerHierarchyIntegrationTests.class,
		BootstrapOrderingCustomPropertySourceIntegrationTests.class,
		RefreshEndpointTests.class, RefreshAutoConfigurationClassPathTests.class,
		RefreshAutoConfigurationMoreClassPathTests.class,
		LifecycleMvcAutoConfigurationTests.class, RefreshAutoConfigurationTests.class,
		RestartIntegrationTests.class, ContextRefresherIntegrationTests.class,
		ContextRefresherTests.class,
		ConfigurationPropertiesRebinderListIntegrationTests.class,
		ConfigurationPropertiesRebinderLifecycleIntegrationTests.class,
		ConfigurationPropertiesRebinderIntegrationTests.class,
		ConfigurationPropertiesRebinderRefreshScopeIntegrationTests.class,
		ConfigurationPropertiesRebinderProxyIntegrationTests.class,
		RefreshScopeIntegrationTests.class, RefreshScopeConfigurationTests.class,
		RefreshScopeLazyIntegrationTests.class, ImportRefreshScopeIntegrationTests.class,
		RefreshScopeConcurrencyTests.class, RefreshEndpointIntegrationTests.class,
		RefreshScopeConfigurationScaleTests.class,
		RefreshScopeNullBeanIntegrationTests.class,
		MoreRefreshScopeIntegrationTests.class,
		RefreshScopeListBindingIntegrationTests.class,
		RefreshScopeWebIntegrationTests.class, RefreshScopePureScaleTests.class,
		RefreshScopeScaleTests.class, RefreshScopeSerializationTests.class,
		NamedContextFactoryTests.class, EnvironmentManagerIntegrationTests.class,
		EnvironmentManagerTest.class, LoggingRebinderTests.class,
		RefreshScopeHealthIndicatorTests.class })
@Ignore
public class AdhocTestSuite {

}
