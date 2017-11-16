package gate.test;

import org.junit.BeforeClass;

public class GATEPluginTests {

  @BeforeClass
  public void loadPluginUnderTest() throws Exception {
    GATEPluginTestCase.loadPlugin();
  }
}
