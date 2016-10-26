package org.springframework.cloud;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Taken from Spring Boot test utils.
 * https://github.com/spring-projects/spring-boot/blob/1.4.x/spring-boot/src/test/java/org/springframework/boot/testutil/FilteredClassPathRunner.java
 * @author Ryan Baxter
 */
public class FilteredClassPathRunner extends BlockJUnit4ClassRunner {
	public FilteredClassPathRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected TestClass createTestClass(Class<?> testClass) {
		try {
			ClassLoader classLoader = createTestClassLoader(testClass);
			return new FilteredTestClass(classLoader, testClass.getName());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private URLClassLoader createTestClassLoader(Class<?> testClass) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
		return new FilteredClassLoader(filterUrls(extractUrls(classLoader), testClass),
				classLoader.getParent(), classLoader);
	}

	private URL[] extractUrls(URLClassLoader classLoader) throws Exception {
		List<URL> extractedUrls = new ArrayList<URL>();
		for (URL url : classLoader.getURLs()) {
			if (isSurefireBooterJar(url)) {
				extractedUrls.addAll(extractUrlsFromManifestClassPath(url));
			}
			else {
				extractedUrls.add(url);
			}
		}
		return extractedUrls.toArray(new URL[extractedUrls.size()]);
	}

	private boolean isSurefireBooterJar(URL url) {
		return url.getPath().contains("surefirebooter");
	}

	private List<URL> extractUrlsFromManifestClassPath(URL booterJar) throws Exception {
		List<URL> urls = new ArrayList<URL>();
		for (String entry : getClassPath(booterJar)) {
			urls.add(new URL(entry));
		}
		return urls;
	}

	private String[] getClassPath(URL booterJar) throws Exception {
		JarFile jarFile = new JarFile(new File(booterJar.toURI()));
		try {
			return StringUtils.delimitedListToStringArray(jarFile.getManifest()
					.getMainAttributes().getValue(Attributes.Name.CLASS_PATH), " ");
		}
		finally {
			jarFile.close();
		}
	}

	private URL[] filterUrls(URL[] urls, Class<?> testClass) throws Exception {
		ClassPathEntryFilter filter = new ClassPathEntryFilter(testClass);
		List<URL> filteredUrls = new ArrayList<URL>();
		for (URL url : urls) {
			if (!filter.isExcluded(url)) {
				filteredUrls.add(url);
			}
		}
		return filteredUrls.toArray(new URL[filteredUrls.size()]);
	}

	/**
	 * Filter for class path entries.
	 */
	private static final class ClassPathEntryFilter {

		private final List<String> exclusions;

		private final AntPathMatcher matcher = new AntPathMatcher();

		private ClassPathEntryFilter(Class<?> testClass) throws Exception {
			ClassPathExclusions exclusions = AnnotationUtils.findAnnotation(testClass,
					ClassPathExclusions.class);
			this.exclusions = exclusions == null ? Collections.<String>emptyList()
					: Arrays.asList(exclusions.value());
		}

		private boolean isExcluded(URL url) throws Exception {
			if (!"file".equals(url.getProtocol())) {
				return false;
			}
			String name = new File(url.toURI()).getName();
			for (String exclusion : this.exclusions) {
				if (this.matcher.match(exclusion, name)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Filtered version of JUnit's {@link TestClass}.
	 */
	private static final class FilteredTestClass extends TestClass {

		private final ClassLoader classLoader;

		FilteredTestClass(ClassLoader classLoader, String testClassName)
				throws ClassNotFoundException {
			super(classLoader.loadClass(testClassName));
			this.classLoader = classLoader;
		}

		@Override
		public List<FrameworkMethod> getAnnotatedMethods(
				Class<? extends Annotation> annotationClass) {
			try {
				return getAnnotatedMethods(annotationClass.getName());
			}
			catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private List<FrameworkMethod> getAnnotatedMethods(String annotationClassName)
				throws ClassNotFoundException {
			Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) this.classLoader
					.loadClass(annotationClassName);
			List<FrameworkMethod> methods = super.getAnnotatedMethods(annotationClass);
			return wrapFrameworkMethods(methods);
		}

		private List<FrameworkMethod> wrapFrameworkMethods(
				List<FrameworkMethod> methods) {
			List<FrameworkMethod> wrapped = new ArrayList<FrameworkMethod>(
					methods.size());
			for (FrameworkMethod frameworkMethod : methods) {
				wrapped.add(new FilteredFrameworkMethod(this.classLoader,
						frameworkMethod.getMethod()));
			}
			return wrapped;
		}

	}

	/**
	 * Filtered version of JUnit's {@link FrameworkMethod}.
	 */
	private static final class FilteredFrameworkMethod extends FrameworkMethod {

		private final ClassLoader classLoader;

		private FilteredFrameworkMethod(ClassLoader classLoader, Method method) {
			super(method);
			this.classLoader = classLoader;
		}

		@Override
		public Object invokeExplosively(Object target, Object... params)
				throws Throwable {
			ClassLoader originalClassLoader = Thread.currentThread()
					.getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.classLoader);
			try {
				return super.invokeExplosively(target, params);
			}
			finally {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}

	}

	private static final class FilteredClassLoader extends URLClassLoader {

		private final ClassLoader junitLoader;

		FilteredClassLoader(URL[] urls, ClassLoader parent, ClassLoader junitLoader) {
			super(urls, parent);
			this.junitLoader = junitLoader;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.startsWith("org.junit")) {
				return this.junitLoader.loadClass(name);
			}
			return super.loadClass(name);
		}

	}
}
