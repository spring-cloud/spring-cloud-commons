package org.springframework.cloud.client.discovery.composite.reactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCompositeDiscoveryClientTests {

	@Mock
	private ReactiveDiscoveryClient discoveryClient1;

	@Mock
	private ReactiveDiscoveryClient discoveryClient2;

	@Test
	public void shouldReturnEmptyFluxOfServices() {
		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(emptyList());

		Flux<String> services = client.getServices();

		StepVerifier.create(services)
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldReturnFluxOfServices() {
		TestPublisher<String> discoveryClient1Publisher = TestPublisher.createCold();
		discoveryClient1Publisher.emit("serviceAFromClient1");
		discoveryClient1Publisher.emit("serviceBFromClient1");
		discoveryClient1Publisher.complete();

		TestPublisher<String> discoveryClient2Publisher = TestPublisher.createCold();
		discoveryClient2Publisher.emit("serviceCFromClient2");
		discoveryClient2Publisher.complete();

		when(discoveryClient1.getServices()).thenReturn(discoveryClient1Publisher.flux());
		when(discoveryClient2.getServices()).thenReturn(discoveryClient2Publisher.flux());

		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(asList(discoveryClient1, discoveryClient2));

		Flux<String> services = client.getServices();

		StepVerifier.create(services)
				.expectNext("serviceAFromClient1")
				.expectNext("serviceBFromClient1")
				.expectNext("serviceCFromClient2")
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldReturnEmptyFluxOfServiceInstances() {
		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(emptyList());

		Flux<ServiceInstance> instances = client.getInstances("service");

		StepVerifier.create(instances)
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		DefaultServiceInstance serviceInstance1 = new DefaultServiceInstance("instance", "service", "localhost", 8080, false);
		DefaultServiceInstance serviceInstance2 = new DefaultServiceInstance("instance2", "service", "localhost", 8080, false);
		TestPublisher<ServiceInstance> discoveryClient1Publisher = TestPublisher.createCold();
		discoveryClient1Publisher.emit(serviceInstance1);
		discoveryClient1Publisher.emit(serviceInstance2);
		discoveryClient1Publisher.complete();

		TestPublisher<ServiceInstance> discoveryClient2Publisher = TestPublisher.createCold();
		discoveryClient2Publisher.complete();

		when(discoveryClient1.getInstances("service")).thenReturn(discoveryClient1Publisher.flux());
		when(discoveryClient2.getInstances("service")).thenReturn(discoveryClient2Publisher.flux());

		ReactiveCompositeDiscoveryClient client = new ReactiveCompositeDiscoveryClient(asList(discoveryClient1, discoveryClient2));

		Flux<ServiceInstance> instances = client.getInstances("service");

		StepVerifier.create(instances)
				.expectNext(serviceInstance1)
				.expectNext(serviceInstance2)
				.expectComplete()
				.verify();
	}

}