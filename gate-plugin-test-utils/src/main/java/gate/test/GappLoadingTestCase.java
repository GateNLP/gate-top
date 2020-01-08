package gate.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import gate.Factory;
import gate.Gate;
import gate.Resource;
import gate.creole.Plugin;
import gate.util.persistence.PersistenceManager;

public abstract class GappLoadingTestCase extends GATEPluginTestCase {

	String[] excluded = null;

	public GappLoadingTestCase(String... excluded) {
		this.excluded = excluded;
	}

	public void testGappLoading() throws Exception {

		URL creoleURL = this.getClass().getResource("/creole.xml");
		URL resourcesURL = new URL(creoleURL, "resources");

		Path pathInPlugin = Paths.get(resourcesURL.toURI());

		Set<Plugin> initialPlugins = new HashSet<Plugin>(Gate.getCreoleRegister().getPlugins());
		Set<Plugin> loadedPlugins = new LinkedHashSet<Plugin>();

		if (Files.exists(pathInPlugin)) {

			Files.walkFileTree(pathInPlugin, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {

					String filename = filePath.getFileName().toString().toLowerCase();
					if (filename.endsWith(".gapp") || filename.endsWith(".xgapp")) {

						boolean shouldTest = true;
						for (String exclude : excluded) {
							shouldTest &= !filePath.endsWith(exclude);
						}

						if (shouldTest) {
							System.out.println("Trying to load " + filePath);
							Object obj = null;
							try {
								obj = PersistenceManager.loadObjectFromFile(filePath.toFile());
								Set<Plugin> loadedByApp = new LinkedHashSet<Plugin>(Gate.getCreoleRegister().getPlugins());
								loadedByApp.removeAll(initialPlugins);
								
								for (Plugin plugin : loadedByApp) {
									Gate.getCreoleRegister().unregisterPlugin(plugin);
								}
								
								loadedPlugins.addAll(loadedByApp);
								
							} catch (Exception e) {
								throw new IOException(e);
							} finally {
								if (obj instanceof Resource) {
									Factory.deleteResource((Resource) obj);
								}
							}

						}
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}

		// Having now loaded all the gapp files we'll dump out the list of loaded
		// plugins so that we can track the dependencies of this plugin. We actually
		// dump two versions. Firstly a plain text file that just lists the plugins,
		// and then a DOT graph file that shows the dependency relations between the
		// plugins

		// Firstly we work out which plugins are root elements in the dependency
		// graph, and at the same time produce the plain text version of the plugins
		// list into target/creole-dependencies.txt
		Set<Plugin> plugins = new LinkedHashSet<Plugin>(Gate.getCreoleRegister().getPlugins());

		try (PrintWriter out = new PrintWriter(new File(
				gate.util.Files.fileFromURL(creoleURL).getParentFile().getParentFile(), "creole-dependencies.txt"))) {

			out.println(generatePluginLabel(pluginUnderTest));
			for (Plugin plugin : Gate.getCreoleRegister().getPlugins()) {

				if (!plugin.equals(pluginUnderTest)) {
					out.println(generatePluginLabel(plugin));
				}

				for (Plugin required : plugin.getRequiredPlugins()) {
					plugins.remove(required);
				}
			}
		}

		// we now loop through all the plugins that are root elements (i.e. they
		// aren't required by another plugin so must be listed in a gapp file). In
		// the output file actual dependencies between plugins are shown in red (r
		// for required) whereas plugins loaded by an gapp (and not required by
		// something else) are shown in green (g for gapp)
		try (PrintWriter out = new PrintWriter(new File(
				gate.util.Files.fileFromURL(creoleURL).getParentFile().getParentFile(), "creole-dependencies.gv"))) {

			out.println("digraph G {");

			out.println("   // ensure we always include this plugin even if no dependencies");
			out.println("   \"" + generatePluginLabel(pluginUnderTest) + "\"\n");

			for (Plugin plugin : plugins) {

				dumpPluginHierarchy(out, plugin, "[color=red]");

				if (!plugin.equals(pluginUnderTest)) {
					StringBuilder builder = new StringBuilder();
					builder.append("   \"").append(generatePluginLabel(pluginUnderTest)).append("\" -> \"")
							.append(generatePluginLabel(plugin)).append("\" [color=green]\n");

					out.println(builder.toString());
				}
			}

			out.println("}");
		}
	}

	/**
	 * Util method for recursively following the required links through a sequence
	 * of plugins
	 */
	private static void dumpPluginHierarchy(PrintWriter out, Plugin plugin, String formatting) {
		StringBuilder builder = new StringBuilder();
		for (Plugin required : plugin.getRequiredPlugins()) {
			builder.append("   \"").append(generatePluginLabel(plugin)).append("\" -> \"")
					.append(generatePluginLabel(required)).append("\"");
			if (formatting != null && !formatting.isEmpty())
				builder.append(" ").append(formatting);
			builder.append("\n");
		}

		out.print(builder.toString());
	}

	/**
	 * Util method to build a textual label for a given plugin
	 */
	private static String generatePluginLabel(Plugin plugin) {
		StringBuilder builder = new StringBuilder();
		if (plugin instanceof Plugin.Maven) {
			Plugin.Maven mavenPlugin = (Plugin.Maven) plugin;
			builder.append(mavenPlugin.getGroup()).append(":").append(mavenPlugin.getArtifact()).append(":")
					.append(mavenPlugin.getVersion());
		} else {
			builder.append(plugin.getClass()).append(":").append(plugin.getName()).append(":")
					.append(plugin.getVersion());
		}

		return builder.toString();
	}
}
