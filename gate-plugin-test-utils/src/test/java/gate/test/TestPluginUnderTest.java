package gate.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import gate.creole.Plugin;

public class TestPluginUnderTest {

  @Test
  public void pluginEquality() throws Exception {
    Plugin testPlugin = new GATEPluginTestCase.PluginUnderTest(new File("creole.xml"), "uk.ac.gate.plugins", "annie", "8.5");
    
    Plugin realPlugin = new Plugin.Maven("uk.ac.gate.plugins", "annie", "8.5");
    
    assertTrue(testPlugin.equals(realPlugin));
    assertTrue(realPlugin.equals(testPlugin));
    
    assertEquals(testPlugin.hashCode(), realPlugin.hashCode());
    
    Set<Plugin> plugins = new HashSet<Plugin>();
    
    plugins.add(testPlugin);
    
    assertTrue(plugins.contains(testPlugin));
    assertTrue(plugins.contains(realPlugin));
  }
}
