package org.springframework.cloud.loadbalancer.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Hash.Jang
 */
@ConfigurationProperties("spring.cloud.loadbalancer.filter")
public class LoadBalancerFilterProperties {
    private List<String> list;

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }
}
