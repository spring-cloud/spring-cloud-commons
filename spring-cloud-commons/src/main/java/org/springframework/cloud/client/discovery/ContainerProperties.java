package org.springframework.cloud.client.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * Container related properties which can be useful context of discovery
 *
 * @author Stefan Gross
 *
 */
@ConfigurationProperties(ContainerProperties.PREFIX)
public class ContainerProperties {

    public static final String PREFIX = "spring.cloud.container";
    private final Environment environment;


    /**
     * Port number to which server.port is mapped in host of container
     *
     * Used for creating a more meaningful instance id without massive changes
     * No other function at the moment
     *
     */
    @Value("${spring.cloud.container.hostPort:$CONTAINER_HOST_PORT}")
    private Integer hostPort;



    private ContainerProperties() {
        this.environment = null; // never called
    }


    public ContainerProperties(Environment environment) {
        this.environment = environment;

    }

    public Integer getHostPort() {
        return hostPort;
    }

    public void setHostPort(Integer hostPort) {
        this.hostPort = hostPort;
    }

    @Override
    public String toString() {
        return "ContainerProperties [hostPort=" + this.hostPort + "]";
    }

}
