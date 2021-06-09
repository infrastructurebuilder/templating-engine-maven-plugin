/*
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infrastructurebuilder.templating.maven;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultMavenProjectHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.templating.TemplatingEngine;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.TemplatingEngineSupplier;
import org.infrastructurebuilder.templating.velocity.VelocityEngineSupplier;
import org.infrastructurebuilder.util.core.IBUtils;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.joor.Reflect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IterativeResourcesTemplatingEngineMojoTest extends AbstractPlatformTest {

  private static final String                       VELOCITY     = "velocity";
  private static final String                       DEF          = "def";
  private static final Path                         i2Properties = tps.getTestClasses().resolve("i2.properties");

  private IterativeResourcesTemplatingEngineMojo    m;
  private IterativeTemplatingComponent              comp;
  private Map<String, String>                       properties;
  private HashMap<String, Object>                   mso;
  private MavenProject                              project;
  private MojoExecution                             mojo;
  private MojoDescriptor                            mojoDesc;
  private Path                                      outputDirectory;
  private HashMap<String, TemplatingEngineSupplier> suppliers;
  private TemplatingEngineSupplier                  tes;
  private List<Platform>                            platforms;
  private Logger                                    logger       = new ConsoleLogger(1, "testcase");
  private Path                                      work, work1, work2, work3, work4;
  private ClassifierNameMapper                      cnm;
  private ClassifierNameMapper                      c2;

  @BeforeEach
  void setUp() throws Exception {
    super.setUp();
    tes = new VelocityEngineSupplier();
    suppliers = new HashMap<>();
    suppliers.put(VELOCITY, tes);
    m = new IterativeResourcesTemplatingEngineMojo();
    m.suppliers = suppliers;
    m.setComp(null);
    comp = new IterativeTemplatingComponent();
    comp.enableLogging(logger);
    mojoDesc = new MojoDescriptor();
    mojo = new MojoExecution(mojoDesc, DEF);
    m.mojo = mojo;
    m.setIcomp(comp);
    properties = new HashMap<>();
    properties.put("X", "Z");
    mso = new HashMap<>();
    mso.put("X", "Zprime");
    project = new MavenProject();
    m.project = project;
    m.source = tps.getTestClasses().resolve("temp").resolve("src").resolve("main").resolve("resources").toFile();

    work = tps.get();
    work1 = work.resolve(UUID.randomUUID().toString() + "1.properties");
    work2 = work.resolve(UUID.randomUUID().toString() + "2.properties");
    work3 = work.resolve(UUID.randomUUID().toString() + "3.properties");
    work4 = work.resolve(UUID.randomUUID().toString() + "4.properties");
    IBUtils.copy(i2Properties, work1);
    IBUtils.copy(i2Properties, work2);
    IBUtils.copy(i2Properties, work3);
    IBUtils.copy(i2Properties, work4);
    outputDirectory = tps.get();
    m.outputDirectory = outputDirectory.toFile();
    p.addInstance(i1);
    i1.setProperties(new Properties());
    Properties i2p = new Properties();
    i2p.setProperty("one", "B");
    i2 = new PlatformInstance(Z, null);
    i2.setProperties(i2p);
    p.addInstance(i2);
    platforms = List.of(p);
  }

  @AfterEach
  void tearDown() throws Exception {
  }

  IterativeResourcesTemplatingPackageMojo getPackageMojo() throws Exception {
    IterativeResourcesTemplatingPackageMojo irtpm;
    DefaultMavenProjectHelper               ph;
    DefaultArtifactHandlerManager           ahm;
    DefaultArtifactHandler                  jh;
    Model                                   model;
    MavenProject                            project;
    Build                                   build;
    Properties                              mvnProps;
    ClassifierNameMapper                    cnm;
    ClassifierNameMapper                    c2;
    JarArchiver                             archiver;
    irtpm = new IterativeResourcesTemplatingPackageMojo();
    ahm = new DefaultArtifactHandlerManager();
    Map<String, ArtifactHandler> m = new HashMap<>();
    jh = new DefaultArtifactHandler("jar");
    jh.setExtension("jar");
    jh.setAddedToClasspath(true);
    jh.setIncludesDependencies(true);
    jh.setLanguage("java");
    mvnProps = new Properties();
    m.put("jar", jh);
    ahm.addHandlers(m);
    ph = new DefaultMavenProjectHelper();
    Reflect.on(ph).set("artifactHandlerManager", ahm);
    model = new Model();
    model.setArtifactId("X");
    model.setGroupId("Y");
    model.setPackaging("pom");
    build = new Build();
    model.setBuild(build);
    model.setModelEncoding("utf-8");
    model.setModelVersion("4.0.0");
    model.setProperties(mvnProps);
    model.setVersion("0.0.1-SNAPSHOT");
    project = new MavenProject(model, logger);
    Artifact a = new DefaultArtifact("X","Y","0.0.1-SNAPSHOT","compile","jar",null,jh);
    project.setArtifact(a);
    archiver = new JarArchiver();
    irtpm.setLog(new DefaultLog(logger));
    cnm = new ClassifierNameMapper("A", "B");
    c2 = new ClassifierNameMapper("Y", "X");

    Path od = tps.get();
    irtpm.outputDirectory = od.toAbsolutePath().toFile();
    irtpm.finalName = "FinalName";
    irtpm.force = true;
    irtpm.mavenProjectHelper = ph;
    irtpm.archiver = archiver;
    irtpm.project = project;
    irtpm.idMappers = List.of(cnm, c2);
    Path ppr = tps.get();

    return irtpm;
  }

  @Test
  void testExecuteAndPackage() throws Exception {
    // Unconfigured
//    assertThrows(TemplatingEngineException.class, () -> m.execute());
    m.appendExecutionIdentifierToOutput = true;
    m.appendExecutionIdentifierToOutput = true;
    m.caseSensitive = false;
    m.dumpContext = true;
    m.engineHint = VELOCITY;
    m.includeDotFiles = true;
    m.includeEnvironment = true;
    m.includeHidden = true;
    m.includeSystemProperties = true;

    m.files.add(work1.toFile());
    m.filesAppendeds.add(work2.toFile());
    m.fileToPropertiesArray.put("X", work3.toFile());
    m.fileToPropertiesArrayAppended.put("Y", work4.toFile());
    m.properties.putAll(properties);
    m.propertiesAppended.putAll(properties);
    m.propertySuppliersMap.put("Q", () -> mso);

    m.project = project;
    m.sourceExtensions.addAll(Set.of("a", "B"));
    m.useSourceParent = false;
    m.setPluginContext(new HashMap());
    m.setLog(null);

    m.platforms = platforms;
    Path ppr = tps.get();
    m.pathPropertiesRoot = ppr.toFile();
    m.pathPropertiesName = "X";


    try {
      m.execute();
      Map                    pc  = m.getPluginContext();
      List<InternalPlatform> lip = ((List<InternalPlatform>) pc
          .getOrDefault(IterativeTemplatingComponent.ITERATED_RESOURCES, new ArrayList<>()));
      assertEquals(2, lip.size());
      IterativeResourcesTemplatingPackageMojo pack = getPackageMojo();
      pack.setPluginContext(pc);
      pack.execute();
      pc = pack.getPluginContext();
      assertFalse(pc.containsKey(IterativeTemplatingComponent.ITERATED_RESOURCES));
    } catch (Throwable e) {
      fail(e);
    }

  }

}
