package org.apache.catalina.loader;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author dzo
 */
public class ModWebappClassLoader extends WebappClassLoader {
	private static final Log log = LogFactory.getLog(ModWebappClassLoader.class);
	
	private static class URLClassLoaderAdapter extends URLClassLoader {

		public URLClassLoaderAdapter(final URL[] urls) {
			super(urls);
		}

		public URLClassLoaderAdapter(final URL[] urls, final ClassLoader parent) {
			super(urls, parent);
		}

		public URLClassLoaderAdapter(final URL[] urls, final ClassLoader parent, final URLStreamHandlerFactory factory) {
			super(urls, parent, factory);
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.net.URLClassLoader#addURL(java.net.URL)
		 */
		@Override
		public void addURL(final URL url) {
			// just make the method public!
			super.addURL(url);
		}
	}

	private final URLClassLoaderAdapter loader;

	/**
	 * sets up loader
	 */
	public ModWebappClassLoader() {
		super();
		loader = new URLClassLoaderAdapter(new URL[0]);
	}

	/**
	 * @param parent
	 */
	public ModWebappClassLoader(final ClassLoader parent) {
		super(parent);
		loader = new URLClassLoaderAdapter(new URL[0], parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.loader.WebappClassLoader#addRepository(java.lang.
	 * String, java.io.File)
	 */
	@Override
	synchronized void addRepository(final String repository, final File file) {
		/*
		 * don't add classes dir to use uptodate class files from output dir of e.g. eclipse
		 */
		if ("/WEB-INF/classes/".equals(repository)) {
			return;
		}
		super.addRepository(repository, file);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.loader.WebappClassLoader#addRepository(java.lang.
	 * String, java.io.File)
	 */
	@Override
	public void addRepository(final String repository) {
		super.addRepository(repository);
		
		log.debug("added " + repository);
		
		try {
			final URL url = new URL(repository);
			this.loader.addURL(url);
		} catch (final MalformedURLException e) {
			// not reachable
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.loader.WebappClassLoader#findResourceInternal(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	protected ResourceEntry findResourceInternal(final String name, final String path) {
		/*
		 * 1) check the cache
		 */
		final ResourceEntry cachedEntry = resourceEntries.get(name);
		if (cachedEntry != null) {
			return cachedEntry;
		}
		
		/*
		 * 2) try the class paths from sysdeo
		 */
		final URL url = this.loader.findResource(path);
		if (url != null) {
			ResourceEntry entry = new ResourceEntry();
			entry.source = url;

			/*
			 * found insert into cache
			 */
			synchronized (resourceEntries) {
				final ResourceEntry cacheEntry = resourceEntries.get(name);
				if (cacheEntry == null) {
					resourceEntries.put(name, entry);
				} else {
					entry = cacheEntry;
				}
			}
			return entry;
		}
		
		/*
		 * 3) 
		 */
		return super.findResourceInternal(name, path);
	}

	/* (non-Javadoc)
	 * @see org.apache.catalina.loader.WebappClassLoader#findResources(java.lang.String)
	 */
	@Override
	public Enumeration<URL> findResources(final String name) throws IOException {
		final Enumeration<URL> foundResources = this.loader.findResources(name);
		
		if (foundResources != null && foundResources.hasMoreElements()) {
			return foundResources;
		}
		
		return super.findResources(name);
	}
}
