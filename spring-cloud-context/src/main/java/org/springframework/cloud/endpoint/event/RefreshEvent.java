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

package org.springframework.cloud.endpoint.event;

import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.ApplicationEvent;

/**
 * Event that triggers a call to {@link RefreshEndpoint#refresh()}.
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("serial")
public class RefreshEvent extends ApplicationEvent {

	private Object event;

	private String eventDesc;

	public RefreshEvent(Object source, Object event, String eventDesc) {
		super(source);
		this.event = event;
		this.eventDesc = eventDesc;
	}

	public Object getEvent() {
		return this.event;
	}

	public String getEventDesc() {
		return this.eventDesc;
	}

}
