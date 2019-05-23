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

import gate.Factory;
import gate.Gate;
import gate.Resource;
import gate.creole.Plugin;
import gate.util.ant.packager.GappModel.MavenPlugin;
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
		
		if (!Files.exists(pathInPlugin)) return;

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

    try (PrintWriter out = new PrintWriter(new File(
        gate.util.Files.fileFromURL(creoleURL).getParentFile().getParentFile(),
        "required-plugins.txt"))) {
      for(Plugin plugin : Gate.getCreoleRegister().getPlugins()) {
        if(plugin instanceof Plugin.Maven) {
          Plugin.Maven mavenPlugin = (Plugin.Maven)plugin;
          out.println(mavenPlugin.getGroup() + ":" + mavenPlugin.getArtifact()
              + ":" + mavenPlugin.getVersion());
        } else {
          out.println(plugin.getClass() + ": " + plugin.getName() + " "
              + plugin.getVersion());
        }
      }
    }
	}
}
