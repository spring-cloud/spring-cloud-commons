package org.springframework.cloud.client;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Spencer Gibb
 */
public class SingleImplementationImportSelectorTests {

    @Test
    public void testFindAnnotation() {
        MyAnnotationImportSelector selector = new MyAnnotationImportSelector();
        assertEquals("annotationClass was wrong", MyAnnotation.class, selector.annotationClass);
    }

    public static @interface MyAnnotation {}
    public static class MyAnnotationImportSelector extends SingleImplementationImportSelector<MyAnnotation> {  }
}
