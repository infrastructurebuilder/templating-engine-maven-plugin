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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.templating.TemplatingEngine;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.TemplatingEngineSupplier;
import org.infrastructurebuilder.templating.maven.internal.IterativeTemplatingComponent;
import org.infrastructurebuilder.templating.maven.internal.TemplatingComponent;
import org.infrastructurebuilder.templating.velocity.VelocityEngineSupplier;
import org.infrastructurebuilder.util.core.IBUtils;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IterativeResourcesTemplatingEngineMojoTest extends AbstractPlatformTest {

  private static final String              VELOCITY     = "velocity";
  private static final String              DEF          = "def";
  private static final Path                i2Properties = tps.getTestClasses().resolve("i2.properties");


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
  private List<Platform> platforms;
  private Logger logger  = new ConsoleLogger(1,"testcase");
  private Path work, work1, work2, work3, work4;

  @BeforeEach
  void setUp() throws Exception {
    super.setUp();
    tes = new VelocityEngineSupplier();
    suppliers = new HashMap<>();
    suppliers.put(VELOCITY, tes);
    m = new IterativeResourcesTemplatingEngineMojo();
    m.suppliers = suppliers;
    comp = new IterativeTemplatingComponent();
    comp.enableLogging(logger);
    mojoDesc = new MojoDescriptor();
    mojo = new MojoExecution(mojoDesc, DEF);
    m.mojo = mojo;
    m.setComp(comp);
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
    i2 = new PlatformInstance(Z,null);
    i2.setProperties(i2p);
    p.addInstance(i2);
    platforms = List.of( p);
  }

  @AfterEach
  void tearDown() throws Exception {
  }

  @Test
  void testExecute() {
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

    m.platforms = platforms;


    try {
      m.execute();
    } catch (Throwable e) {
      fail(e);
    }

  }

}
