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

package org.springframework.cloud.context.environment;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EnvironmentManagerTest {

	@Test
	public void testCorrectEvents() {
		MockEnvironment environment = new MockEnvironment();
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		EnvironmentManager environmentManager = new EnvironmentManager(environment);
		environmentManager.setApplicationEventPublisher(publisher);

		environmentManager.setProperty("foo", "bar");

		then(environment.getProperty("foo")).isEqualTo("bar");
		ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor
				.forClass(ApplicationEvent.class);
		verify(publisher, times(1)).publishEvent(eventCaptor.capture());
		then(eventCaptor.getValue()).isInstanceOf(EnvironmentChangeEvent.class);
		EnvironmentChangeEvent event = (EnvironmentChangeEvent) eventCaptor.getValue();
		then(event.getKeys()).containsExactly("foo");

		reset(publisher);

		environmentManager.reset();
		then(environment.getProperty("foo")).isNull();
		verify(publisher, times(1)).publishEvent(eventCaptor.capture());
		then(eventCaptor.getValue()).isInstanceOf(EnvironmentChangeEvent.class);
		event = (EnvironmentChangeEvent) eventCaptor.getValue();
		then(event.getKeys()).containsExactly("foo");
	}

}
