package gate.test;

import org.junit.BeforeClass;

import gate.creole.Plugin;

public class GATEPluginTests {

  protected static Plugin.Maven pluginUnderTest = null;

  @BeforeClass
  public static void loadPluginUnderTest() throws Exception {
    pluginUnderTest = GATEPluginTestCase.loadPlugin();
  }
}
