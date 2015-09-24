package org.springframework.cloud.util;

import static org.junit.Assert.*;

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
		env = new MockEnvironment();
	}

	@After
	public void destroy() {
		env = null;
	}

	@Test
	public void emptyEnvironmentWorks() {
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertNull("instanceId was not null", instanceId);
	}

	@Test
	public void vcapInstanceIdWorks() {
		env.setProperty("vcap.application.instance_id", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void hostnameWorks() {
		env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void appNameWorks() {
		env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void hostnameAndAppNameWorks() {
		env.setProperty("spring.application.name", DEFAULT_ID);
		env.setProperty("spring.cloud.client.hostname", DEFAULT_ID+"2");
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID+"2"+":"+DEFAULT_ID, instanceId);
	}

	@Test
	public void instanceIdWorks() {
		env.setProperty("spring.cloud.client.hostname", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void portWorks() {
		env.setProperty("spring.application.name", DEFAULT_ID);
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID, instanceId);
	}

	@Test
	public void appNameAndPortWorks() {
		env.setProperty("spring.application.name", DEFAULT_ID);
		env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", DEFAULT_ID+":80", instanceId);
	}

	@Test
	public void fullWorks() {
		env.setProperty("spring.cloud.client.hostname", "myhost");
		env.setProperty("spring.application.name", DEFAULT_ID);
		env.setProperty("server.port", "80");
		String instanceId = IdUtils.getDefaultInstanceId(env);
		assertEquals("instanceId was wrong", "myhost:"+DEFAULT_ID+":80", instanceId);
	}

}
