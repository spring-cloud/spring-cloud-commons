/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.commons.util;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * Wrapper that downstream projects can use to keep {@link ThreadPoolTaskScheduler} local.
 *
 * Implementation adapted from Spring Vault.
 *
 */
public class TaskSchedulerWrapper<T extends TaskScheduler>
		implements InitializingBean, DisposableBean {

	private final T taskScheduler;

	public TaskSchedulerWrapper(T taskScheduler) {
		Assert.notNull(taskScheduler, "ThreadPoolTaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	public T getTaskScheduler() {
		return this.taskScheduler;
	}

	@Override
	public void destroy() throws Exception {
		if (DisposableBean.class.isInstance(taskScheduler)) {
			((DisposableBean) this.taskScheduler).destroy();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (InitializingBean.class.isInstance(taskScheduler)) {
			((InitializingBean) this.taskScheduler).afterPropertiesSet();
		}
	}

}
