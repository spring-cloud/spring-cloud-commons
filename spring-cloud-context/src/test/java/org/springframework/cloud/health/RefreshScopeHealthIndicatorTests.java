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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.scope.refresh.RefreshScope;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 */
public class RefreshScopeHealthIndicatorTests {

	@SuppressWarnings("unchecked")
	private ObjectProvider<RefreshScope> scopeProvider = mock(ObjectProvider.class);

	private ConfigurationPropertiesRebinder rebinder = mock(ConfigurationPropertiesRebinder.class);

	private RefreshScope scope = mock(RefreshScope.class);

	private RefreshScopeHealthIndicator indicator = new RefreshScopeHealthIndicator(this.scopeProvider, this.rebinder);

	@BeforeEach
	public void init() {
		BDDMockito.willReturn(this.scope).given(this.scopeProvider).getIfAvailable();
		when(this.rebinder.getErrors()).thenReturn(Collections.emptyMap());
		when(this.scope.getErrors()).thenReturn(Collections.emptyMap());
	}

	@Test
	public void sunnyDay() {
		then(this.indicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void binderError() {
		when(this.rebinder.getErrors()).thenReturn(Collections.singletonMap("foo", new RuntimeException("FOO")));
		then(this.indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void scopeError() {
		when(this.scope.getErrors()).thenReturn(Collections.singletonMap("foo", new RuntimeException("FOO")));
		then(this.indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void bothError() {
		when(this.rebinder.getErrors()).thenReturn(Collections.singletonMap("foo", new RuntimeException("FOO")));
		when(this.scope.getErrors()).thenReturn(Collections.singletonMap("bar", new RuntimeException("BAR")));
		then(this.indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void nullRefreshScope() {
		ObjectProvider<RefreshScope> scopeProvider = mock(ObjectProvider.class);
		BDDMockito.willReturn(null).given(scopeProvider).getIfAvailable();
		then(this.indicator.health().getStatus()).isEqualTo(Status.UP);
	}

}
