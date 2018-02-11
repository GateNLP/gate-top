/*
 * This is a utility script to fetch the list of plugins that get built by the
 * Jenkins GATE-Plugins job, clone them all into one directory and create an
 * aggregator Maven POM that will build them all in one go in an appropriate
 * order taking into account plugin-to-plugin compile dependencies.
 *
 * You then need to:
 * - remove from the generated aggregator POM any plugin modules that cannot
 *   be released to Central (if they depend on libraries that are not
 *   already in central)
 * - branch each plugin for the new release
 *   - for plg in gateplugin-* ; do ( cd $plg ; git checkout -b {version-number} ) ; done
 * - edit the POM files, creole.xml files, xgapp files, and anywhere else that
 *   refers to the version number
 * - mvn install the top-level POM twice, first with -DskipTests and second
 *   without (certain of the GAPP loading tests introduce awkward dependency
 *   cycles that can't be detected by Maven)
 */

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*

def http = new RESTClient("https://jenkins.gate.ac.uk")

def plugins = http.get(path:"/job/GATE-Plugins/api/xml", contentType:XML, query:[xpath:'organizationFolder/job/name', wrapper:'jobs']).data.name*.text()

println "Found ${plugins.size()} plugins"

ProcessBuilder git = new ProcessBuilder()
git.inheritIO()
def gitArgs = { pluginName ->  }

plugins.each { pluginName ->
  println "cloning ${pluginName}"
  git.command(['git', 'clone', "git@github.com:GateNLP/${pluginName}.git".toString()])
  def gitProc = git.start()
  git.waitFor()
}

println "Writing aggregator POM"
new File("pom.xml").withPrintWriter('UTF-8') { w ->
  w << """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>uk.ac.gate</groupId>
  <artifactId>plugins-all</artifactId>
  <version>0.1-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
"""
  plugins.each { pluginName ->
    w.println("    <module>$pluginName</module")
  }
  w << """\
  </modules>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-deploy-plugin</artifactId>
        <configuration>
          <skip>true</skip>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
"""
}

println "Done - check the POM and remove any plugins that can't be deployed to central"
