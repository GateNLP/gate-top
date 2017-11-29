/*
 * DumpCreoleToXML.java
 *
 * Copyright (c) 2016, The University of Sheffield. See the file COPYRIGHT.txt
 * in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 3, June 2007
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Mark A. Greenwood, 19th September 2016
 */

package uk.ac.gate.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import gate.Gate;
import gate.Gate.ResourceInfo;
import gate.creole.Plugin;
import gate.creole.CreoleAnnotationHandler;
import gate.util.GateClassLoader;
import gate.util.Strings;
import gate.util.asm.ClassReader;

/**
 * This Maven plugin creates a fully expanded copy of creole.xml and stores it
 * inside META-INF/gate. This copy is useful for interoperability with other
 * frameworks which might want to know the resources contained within the plugin
 * without needing an instance of GATE to extract the information via the API.
 * Note that the generated file is for information only and in no way effects
 * the operation of the plugin within GATE.
 * 
 * @author Mark A. Greenwood
 **/
@Mojo(name = "DumpCreoleToXML", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DumpCreoleToXML extends AbstractMojo {
  private XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @SuppressWarnings("unchecked")
  public void execute() throws MojoExecutionException {

    // get the output folder from the project (i.e. where everything goes before
    // the jar is built)
    File dir = new File(project.getBuild().getOutputDirectory());

    // the normal creole.xml file which ends up at the root of the jar
    File creoleXML = new File(dir, "creole.xml");

    // if there is no creole.xml then we quit as there is clearly nothing for us
    // to do so why bother carrying on
    if(creoleXML == null || !creoleXML.exists()) return;

    // this is the file we are going to create...
    File expandedXML = new File(dir, "META-INF/gate/creole.xml");

    // ...and we need to know there is somewhere to write it to
    expandedXML.getParentFile().mkdirs();

    // we'll need a temporary classloader to hold any referenced jars
    GateClassLoader cl = null;

    // if we can open the output file for writing then...
    try (FileOutputStream fos = new FileOutputStream(expandedXML);) {

      // initialise GATE and get a temportary classloader
      Gate.init();
      cl = Gate.getClassLoader().getDisposableClassLoader(dir.toString());

      // add the output directory to the classloader so we can access the
      // classes in the plugin we are currently processing
      cl.addURL(dir.toURI().toURL());

      for(Artifact artifact : (Set<Artifact>)project.getArtifacts()) {
        // for each dependency of the plugin try and get a URL to it's jar file
        // and add that to the classloader
        
        //if this doesn't work in all cases try
        //http://aether.jcabi.com/example-classpath.html

        // why do we need this null check when running under jenkins?
        if(artifact != null && artifact.getFile() != null) {
          cl.addURL(artifact.getFile().toURI().toURL());
        }
      }

      // Now create a plugin...
      Plugin plugin = new TargetPlugin(dir, project.getGroupId(),
          project.getArtifactId(), project.getVersion());

      // and a creole annotation handler
      CreoleAnnotationHandler annotationHandler =
          new CreoleAnnotationHandler(plugin);

      // get a handle on the existing creole.xml file
      Document creoleDoc = plugin.getCreoleXML();

      // process the java annotations to add them to the XML file
      annotationHandler.processAnnotations(creoleDoc);

      // save the document to the file
      outputter.output(creoleDoc, fos);
    } catch(Exception e) {
      // goodness knows what happened so just throw up our hands in defeat and
      // chuck the exception back
      throw new MojoExecutionException("error expanding creole", e);
    } finally {
      // forget the classloader so we don't end up with lots of class
      // definitions hanging around that we don't need
      Gate.getClassLoader().forgetClassLoader(cl);
    }
  }

  /**
   * A plugin type we can use to extract information from an existing creole.xml
   * file and a directory of classes, while pretending to load the entire thing
   * from a Maven repository.
   */
  @SuppressWarnings("serial")
  private static class TargetPlugin extends Plugin.Maven {

    private File creoleFile;

    public TargetPlugin(File dir, String group, String artifact, String version)
        throws MalformedURLException {
      super(group, artifact, version);

      this.baseURL = dir.toURI().toURL();
      this.creoleFile = new File(dir, "creole.xml");
    }

    @Override
    public Document getCreoleXML() throws Exception {
      // load the existing creole.xml file into memory
      SAXBuilder builder = new SAXBuilder(false);
      Document jdomDoc = builder.build(new FileInputStream(creoleFile),
          getBaseURL().toExternalForm());
      Element creoleRoot = jdomDoc.getRootElement();

      // add a comment to make it clear the expanded version is just for info
      Comment comment = new Comment(
          "this file is auto-generated, modifications will have no effect");
      creoleRoot.addContent(0, comment);

      // get the full directory path, ending with a separator
      String dir = creoleFile.getParent();
      if(!dir.endsWith(File.separator)) dir = dir + File.separator;

      // recurse trhough the folder finding all the creole resources
      Set<String> resources = new HashSet<String>();
      scanDir(dir.length(), creoleFile.getParentFile(), resources);

      for(String resource : resources) {
        // for each creole resource...

        // create a new entry in the XML file so that we know which classes to
        // scan for further information
        Element resourceElement = new Element("RESOURCE");
        Element classElement = new Element("CLASS");
        classElement.setText(resource);
        resourceElement.addContent(classElement);
        creoleRoot.addContent(resourceElement);
      }
      
      //return the part expanded creole.xml file
      return jdomDoc;
    }

    /**
     * Recursively scan through a directory structure to find all class files
     * and store the names of those which are creole resources
     * 
     * @param prefix
     *          the number of characters to remove from the beginning of the
     *          path in order to convert the path into a fully specified
     *          classname
     * @param dir
     *          the current directory to scan for class files
     * @param resources
     *          a set containing the classnames of any creole resources found so
     *          far
     * @throws IOException
     *           if an exception occurs reading a class file
     */
    private void scanDir(int prefix, File dir, Set<String> resources)
        throws IOException {

      for(File file : dir.listFiles()) {
        // for each file in the current directory...

        if(file.isDirectory()) {
          // ... if it's a directory recurse into it
          scanDir(prefix, file, resources);
        } else if(file.getName().endsWith((".class"))) {
          // ... if it's a class file convert the path to a classname
          String className = file.getAbsolutePath().substring(prefix);
          className =
              className.substring(0, className.length() - 6).replaceAll(Pattern.quote(Strings.getFileSep()), ".");

          // access the class so we can extract any annotations etc. from it
          ClassReader classReader = new ClassReader(new FileInputStream(file));
          ResourceInfo resInfo = new ResourceInfo(null, className, null);
          ResourceInfoVisitor visitor = new ResourceInfoVisitor(resInfo);
          classReader.accept(visitor, ClassReader.SKIP_CODE
              | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

          // if the class is a creole resource store the classname
          if(visitor.isCreoleResource()) {
            resources.add(className);
          }
        }
      }
    }
  }
}
