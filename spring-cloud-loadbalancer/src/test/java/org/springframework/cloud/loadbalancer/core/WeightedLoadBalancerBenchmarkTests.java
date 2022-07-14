/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Zhuozhi Ji
 */
@State(Scope.Thread)
@EnabledIfSystemProperty(named = "slowTests", matches = "true")
public class WeightedLoadBalancerBenchmarkTests {

	@Param({ "1", "10", "100", "1000", "10000" })
	int hostCount;

	WeightedLoadBalancer weightedLoadBalancer;

	@Setup
	public void prepare() {
		weightedLoadBalancer = weightedLoadBalancer();
	}

	@Benchmark
	public void weightedLoadBalancerChoose() {
		weightedLoadBalancer.choose().block();
	}

	@Benchmark
	@Threads(Threads.MAX)
	public void weightedLoadBalancerChooseConcurrently() {
		weightedLoadBalancer.choose().block();
	}

	@Test
	void runBenchmarks() throws RunnerException {
		Options options = new OptionsBuilder().include(this.getClass().getName()).mode(Mode.Throughput)
				.warmupTime(TimeValue.seconds(1)).warmupIterations(5).measurementTime(TimeValue.seconds(1))
				.measurementIterations(5).forks(1).shouldDoGC(false) // we don't need gc
				// pause
				.build();

		new Runner(options).run();
	}

	WeightedLoadBalancer weightedLoadBalancer() {
		List<ServiceInstance> instances = IntStream.rangeClosed(1, hostCount).mapToObj(i -> {
			DefaultServiceInstance instance = new DefaultServiceInstance();
			instance.setInstanceId(i + "");
			instance.getMetadata().put("weight", i + "");
			return instance;
		}).collect(Collectors.toList());

		SameInstancePreferenceServiceInstanceListSupplier supplier = mock(
				SameInstancePreferenceServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(instances));
		ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider = new SimpleObjectProvider<>(
				supplier);
		return new WeightedLoadBalancer(serviceInstanceListSupplierProvider, UUID.randomUUID().toString());
	}

}
