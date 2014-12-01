package org.springframework.boot.actuate.autoconfigure;

import static org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration.ManagementServerPort;

import org.springframework.beans.factory.BeanFactory;

/**
 * @author Spencer Gibb
 */
public class ManagementServerPortUtils {

    public static ManagementServerPort get(BeanFactory beanFactory) {
        return ManagementServerPort.get(beanFactory);
    }

    public static boolean isDifferent(BeanFactory beanFactory) {
        return get(beanFactory) == ManagementServerPort.DIFFERENT;
    }

    public static boolean isDisabled(BeanFactory beanFactory) {
        return get(beanFactory) == ManagementServerPort.DISABLE;
    }

    public static boolean isSame(BeanFactory beanFactory) {
        return get(beanFactory) == ManagementServerPort.SAME;
    }
}
