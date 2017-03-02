package gate.test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import gate.Factory;
import gate.Resource;
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

	}
}
