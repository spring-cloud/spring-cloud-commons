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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * Wrapper that downstream projects can use to keep {@link ThreadPoolTaskScheduler} local.
 *
 * Implementation taken from Spring Vault.
 *
 */
public class TaskSchedulerWrapper implements InitializingBean, DisposableBean {

	private final ThreadPoolTaskScheduler taskScheduler;

	private final boolean acceptAfterPropertiesSet;

	private final boolean acceptDestroy;

	public TaskSchedulerWrapper(ThreadPoolTaskScheduler taskScheduler) {
		this(taskScheduler, true, true);
	}

	protected TaskSchedulerWrapper(ThreadPoolTaskScheduler taskScheduler,
			boolean acceptAfterPropertiesSet, boolean acceptDestroy) {

		Assert.notNull(taskScheduler, "ThreadPoolTaskScheduler must not be null");

		this.taskScheduler = taskScheduler;
		this.acceptAfterPropertiesSet = acceptAfterPropertiesSet;
		this.acceptDestroy = acceptDestroy;
	}

	/**
	 * Factory method to adapt an existing {@link ThreadPoolTaskScheduler} bean without
	 * calling lifecycle methods.
	 * @param scheduler the actual {@code ThreadPoolTaskScheduler}.
	 * @return the wrapper for the given {@link ThreadPoolTaskScheduler}.
	 * @see #afterPropertiesSet()
	 * @see #destroy()
	 */
	public static TaskSchedulerWrapper fromInstance(ThreadPoolTaskScheduler scheduler) {
		return new TaskSchedulerWrapper(scheduler, false, false);
	}

	public ThreadPoolTaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	@Override
	public void destroy() {
		if (acceptDestroy) {
			this.taskScheduler.destroy();
		}
	}

	@Override
	public void afterPropertiesSet() {

		if (this.acceptAfterPropertiesSet) {
			this.taskScheduler.afterPropertiesSet();
		}
	}

}
