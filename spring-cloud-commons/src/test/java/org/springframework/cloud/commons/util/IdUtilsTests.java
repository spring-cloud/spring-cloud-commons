package org.springframework.cloud.commons.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Spencer Gibb
 */
public class IdUtilsTests {

	public static final String DEFAULT_ID = "id1";
	private MockEnvironment env;

	@Before
	public void setup() {
		this.env = new MockEnvironment();
	}

	@After
	public void destroy() {
		this.env = null;
	}

	@Test
	public void emptyEnvironmentWorks() {
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertNull("instanceId was not null", instanceId);
	}

	@Test
	public void vcapInstanceIdWorks() {
		this.env.setProperty("vcap.application.instance_id", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void hostnameWorks() {
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void appNameWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void hostnameAndAppNameWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID+"2");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID+"2"+":"+DEFAULT_ID, instanceId);
	}

	@Test
	public void instanceIdWorks() {
		this.env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void portWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void appNameAndPortWorks() {
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", DEFAULT_ID+":80", instanceId);
	}

	@Test
	public void fullWorks() {
		this.env.setProperty("spring.cloud.client.hostname", "myhost");
		this.env.setProperty("spring.application.name", DEFAULT_ID);
		this.env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(this.env);
		assertEquals("instanceId was wrong", "myhost:"+DEFAULT_ID+":80", instanceId);
	}

}
