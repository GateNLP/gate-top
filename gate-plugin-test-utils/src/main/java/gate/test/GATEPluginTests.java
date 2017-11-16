package gate.test;

import org.junit.BeforeClass;

public class GATEPluginTests {

  @BeforeClass
  public static void loadPluginUnderTest() throws Exception {
    GATEPluginTestCase.loadPlugin();
  }
}
