package org.springframework.cloud.client.discovery.noop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class NoopDiscoveryClientAutoConfigurationTest {

    @InjectMocks
    private NoopDiscoveryClientAutoConfiguration target;

    @Mock
    private Environment env;

    @Test
    public void defaultValues() throws UnknownHostException {

        when(env.getProperty("spring.application.name", "application")).thenReturn("name");

        target.init();

        ServiceInstance serviceInstance = (ServiceInstance) ReflectionTestUtils.getField(target, "serviceInstance");

        assertEquals(InetAddress.getLocalHost().getHostName(), serviceInstance.getHost());
        assertEquals(InetAddress.getLocalHost().getHostAddress(), serviceInstance.getAddress());
        assertEquals("name", serviceInstance.getServiceId());

    }

}