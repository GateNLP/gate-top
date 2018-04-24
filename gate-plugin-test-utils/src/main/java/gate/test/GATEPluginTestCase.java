/*
 *  GATEPluginTestCase.java
 *
 *  Copyright (c) 2016, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Mark A. Greenwood, 13th April 2016
 */

package gate.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import gate.Gate;
import gate.Gate.ResourceInfo;
import gate.creole.Plugin;
import gate.util.Files;
import gate.util.asm.ClassReader;
import gate.creole.ResourceInstantiationException;
import junit.framework.TestCase;

public class GATEPluginTestCase extends TestCase {

  @Override
  public void setUp() throws Exception {
    loadPlugin();
  }
  
	protected static void loadPlugin() throws Exception {

		if (!Gate.isInitialised()) {
			Gate.runInSandbox(true);
			Gate.init();
		}

		URL creoleUrl = GATEPluginTestCase.class.getResource("/creole.xml");
		if(creoleUrl == null) {
		  throw new ResourceInstantiationException("Could not load plugin, creole.xml not found");
		}
		Properties properties = new Properties();
		URL propsUrl = GATEPluginTestCase.class.getResource("/creole.properties");
		if(propsUrl == null) {
		  throw new ResourceInstantiationException("Could not load plugin, creole.properties not found");
		}
		InputStream propsStream = propsUrl.openStream();
		properties.load(propsStream);

		Plugin plugin = new PluginUnderTest(Files.fileFromURL(creoleUrl),
				properties.getProperty("groupId"),
				properties.getProperty("artifactId"),
				properties.getProperty("version"));

		if (!Gate.getCreoleRegister().getPlugins().contains(plugin)) {
			Gate.getCreoleRegister().registerPlugin(plugin);
		}
	}

	static class PluginUnderTest extends Plugin.Maven {

    private static final long serialVersionUID = -7173026992962397847L;

    private File creoleFile;

		public PluginUnderTest(File creoleFile, String group, String artifact,
				String version) throws MalformedURLException {
			super(group, artifact, version);
			this.creoleFile = creoleFile;
			this.baseURL = (new URL(creoleFile.toURI().toURL(), "."));
		}

		/**
		 * Ignore all the clever handling of -creole.jar in normal Maven plugins
		 * and just delegate back to the standard getCreoleXML()
		 * @return the parsed creole.xml from the plugin under test.
		 * @throws Exception if anything goes wrong during parsing
		 */
		@Override
		public Document getMetadataXML() throws Exception {
			return getCreoleXML();
		}

		@Override
		public Document getCreoleXML() throws Exception {
			SAXBuilder builder = new SAXBuilder(false);

			Document jdomDoc = builder.build(new FileInputStream(creoleFile),
					getBaseURL().toExternalForm());

			Element creoleRoot = jdomDoc.getRootElement();

			Set<String> resources = new HashSet<String>();

			String dir = creoleFile.getParent();
			if (!dir.endsWith(File.separator))
				dir = dir + File.separator;

			scanDir(dir.length(), creoleFile.getParentFile(), resources);

			for (String resource : resources) {
				Element resourceElement = new Element("RESOURCE");
				Element classElement = new Element("CLASS");
				classElement.setText(resource);
				resourceElement.addContent(classElement);
				creoleRoot.addContent(resourceElement);
			}

			return jdomDoc;
		}

		private void scanDir(int prefix, File dir, Set<String> resources)
				throws IOException {

			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					scanDir(prefix, file, resources);
				} else if (file.getName().endsWith((".class"))) {

					String className = file.getAbsolutePath().substring(prefix);
					className = className.substring(0, className.length() - 6)
							.replace('/', '.');

					ClassReader classReader = new ClassReader(
							new FileInputStream(file));

					ResourceInfo resInfo = new ResourceInfo(null, className,
							null);
					ResourceInfoVisitor visitor = new ResourceInfoVisitor(
							resInfo);

					classReader.accept(visitor, ClassReader.SKIP_CODE
							| ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					if (visitor.isCreoleResource()) {
						resources.add(className);
					}
				}
			}

		}

	}
}
