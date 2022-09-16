/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.client.hedger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Annotation for a method invocation that is hedge-able. Hedging requests is a way to retry requests that have long
 * worst-case response times. If a request is known to be safe to issue multiple times, we will fire off multiple
 * requests, potentially after some initial delay.
 *
 * This is useful to try to cut down on the long tail latencies of requests. For example, one could set the delay
 * to the 95th percentile latency of the downstream service. Typically that will result in a significant reduction of
 * 95th percentile latency in exchange for 2-5% traffic increase. (The actual numbers depend on the distribution of
 * latencies.)
 *
 * {@see http://accelazh.github.io/storage/Tail-Latency-Study} for more background on how hedging works.
 *
 * @author Csaba Kos
 * @author Kevin Binswanger
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Qualifier
public @interface Hedged {

}
