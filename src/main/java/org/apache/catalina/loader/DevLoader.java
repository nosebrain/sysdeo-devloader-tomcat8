package org.apache.catalina.loader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Martin Kahr
 * @author dzo
 */
public class DevLoader extends WebappLoader {
	private static final Log log = LogFactory.getLog(DevLoader.class);
	
	private static final String webClassPathFile = ".#webclasspath";
	private static final String tomcatPluginFile = ".tomcatplugin";
	
	/**
	 * default constructor
	 */
	public DevLoader() {
		super();
	}
	
	/**
	 * @param parent the parent class loader
	 */
	public DevLoader(final ClassLoader parent) {
		super(parent);
	}
	
	/**
	 * @see org.apache.catalina.Lifecycle#start()
	 */
	@Override
	public void startInternal() throws LifecycleException {
		log.debug("Starting DevLoader");
		
		// create the class loader
		super.startInternal();
		
		final WebappClassLoader devCl = (WebappClassLoader) super.getClassLoader();
		final WebResourceRoot resourceRoot = devCl.getResources();
		
		final List<String> webClassPathEntries = this.readWebClassPathEntries();
		final StringBuilder classpath = new StringBuilder();
		for (final String entry : webClassPathEntries) {
			File classPathDir = new File(entry);
			if (classPathDir.exists()) {
				// normalize dir path
				if (classPathDir.isDirectory() && !entry.endsWith("/")) {
					classPathDir = new File(entry + "/");
				}
				final WebResourceSet webResourceSet = new DirResourceSet(resourceRoot, "/WEB-INF/classes/", classPathDir.getAbsolutePath(), "/");
				resourceRoot.addPreResources(webResourceSet);
				classpath.append(classPathDir.toString() + File.pathSeparatorChar);
			} else {
				log.error(entry + " does not exist !");
			}
		}
		
		final String cp = (String) this.getServletContext().getAttribute(Globals.CLASS_PATH_ATTR);
		final StringTokenizer tokenizer = new StringTokenizer(cp, String.valueOf(File.pathSeparatorChar));
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			// only on windows
			if ((token.charAt(0) == '/') && (token.charAt(2)==':')) {
				token = token.substring(1);
			}
			classpath.append(token);
			classpath.append(File.pathSeparatorChar);
		}
		this.getServletContext().setAttribute(Globals.CLASS_PATH_ATTR, classpath.toString());
		log.debug("JSPCompiler Classpath = " + classpath);
	}
	
	protected List<String> readWebClassPathEntries() {
		final File prjDir = this.getProjectRootDir();
		if (prjDir == null) {
			return new ArrayList<String>();
		}
		
		log.debug("projectdir = " + prjDir.getAbsolutePath());

		final List<String> rc = this.loadWebClassPathFile(prjDir);
		
		if (rc == null) {
			// should not happen!
			return new ArrayList<String>();
		}
		return rc;
	}
	
	protected File getProjectRootDir() {
		File rootDir = this.getWebappDir();
		final FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return (file.getName().equalsIgnoreCase(webClassPathFile) ||
				        file.getName().equalsIgnoreCase(tomcatPluginFile));
			}
		};
		while (rootDir != null) {
			final File[] files = rootDir.listFiles(filter);
			if ((files != null) && (files.length >= 1)) {
				return files[0].getParentFile();
			}
			rootDir = rootDir.getParentFile();
		}
		return null;
	}
	
	protected List<String> loadWebClassPathFile(final File prjDir) {
		final File cpFile = new File(prjDir, webClassPathFile);
		if (cpFile.exists()) {
			FileReader reader = null;
			try {
				final List<String> rc = new ArrayList<String>();
				reader = new FileReader(cpFile);
				final LineNumberReader lr = new LineNumberReader(reader);
				String line = null;
				while ((line = lr.readLine()) != null) {
					// convert '\' to '/'
					line = line.replace('\\', '/');
					rc.add(line);
				}
				lr.close();
				return rc;
			} catch (final IOException ioEx) {
				if (reader != null) {
					try {
						reader.close();
					} catch (final Exception ignored) {
						// ignore
					}
				}
				return null;
			}
		}
		return null;
	}
	
	protected ServletContext getServletContext() {
	  return this.getContext().getServletContext();
	}
	
	protected File getWebappDir() {
		return new File(this.getServletContext().getRealPath("/"));
	}
}
