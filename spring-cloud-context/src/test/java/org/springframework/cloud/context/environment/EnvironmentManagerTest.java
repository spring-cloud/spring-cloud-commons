package org.springframework.cloud.context.environment;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
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

		assertThat(environment.getProperty("foo")).isEqualTo("bar");
		ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor
				.forClass(ApplicationEvent.class);
		verify(publisher, times(1)).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue()).isInstanceOf(EnvironmentChangeEvent.class);
		EnvironmentChangeEvent event = (EnvironmentChangeEvent) eventCaptor.getValue();
		assertThat(event.getKeys()).containsExactly("foo");

		reset(publisher);

		environmentManager.reset();
		assertThat(environment.getProperty("foo")).isNull();
		verify(publisher, times(1)).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue()).isInstanceOf(EnvironmentChangeEvent.class);
		event = (EnvironmentChangeEvent) eventCaptor.getValue();
		assertThat(event.getKeys()).containsExactly("foo");
	}

}