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
package org.infrastructurebuilder.templating;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.templating.maven.AbstractTemplatingMojo;
import org.infrastructurebuilder.templating.maven.ResourcesTemplatingEngineMojo;
import org.infrastructurebuilder.templating.maven.SourcesTemplatingEngineMojo;
import org.infrastructurebuilder.templating.maven.TestResourcesTemplatingEngineMojo;
import org.infrastructurebuilder.templating.maven.TestsTemplatingEngineMojo;
import org.infrastructurebuilder.templating.maven.internal.TemplatingComponent;
import org.infrastructurebuilder.util.core.WorkingPathSupplier;
import org.joor.Reflect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TemplatingEngineMojoTest {

  private static WorkingPathSupplier wps;
  private static Path                target;
  private static Path                testClasses;

  @BeforeAll
  public static void setupClass() {
    wps = new WorkingPathSupplier();
    target = wps.getRoot();
    testClasses = target.resolve("test-classes");
  }

  @AfterAll
  public static void tearDownClass() {
    wps.finalize();
  }

  Path                                      path;
  Path                                      baseDir;
  String                                    executionIdentifier;
  boolean                                   appendExecutionIdentifierToOutput;
  Map<String, TemplatingEngineSupplier>     suppliers;
  String                                    engine;
  Map<String, String>                       properties;
  Map<String, String>                       propertiesAppended;
  Map<String, File>                         fileToPropertiesArray;
  Map<String, File>                         fileToPropertiesArrayAppended;
  List<File>                                files;
  List<File>                                filesAppendeds;
  File                                      templateSources;
  File                                      templateTestSources;
  File                                      sourcesOutputDirectory;
  File                                      testSourcesOutputDirectory;
  File                                      resourcesOutputDirectory;
  File                                      testResourcesOutputDirectory;
  Set<String>                               sourceExtensions;
  boolean                                   includeDotFiles;
  boolean                                   includeHidden;
  boolean                                   includeEnvironment      = true;
  boolean                                   includeSystemProperties = true;
  boolean                                   dumpContext;
  MavenProject                              project;
  SourcesTemplatingEngineMojo               sm;
  private TestsTemplatingEngineMojo         tm;
  private ResourcesTemplatingEngineMojo     rm;
  private TestResourcesTemplatingEngineMojo tr;
  private Map<String, MSOSupplier>          map;
  private List<String>                      ss;

  @BeforeEach
  public void setUp() throws Exception {
    map = new HashMap<>();
    ss = Arrays.asList("X");
    map.put("X", new DummyAbstractMavenBackedPropertiesSupplier());
    appendExecutionIdentifierToOutput = true;
    baseDir = target.getParent();
    path = wps.get();
    project = new MavenProject();
    properties = new HashMap<>();
    properties.put(TemplatingEngine.EXECUTION_IDENTIFIER, "fakeval");
    propertiesAppended = new HashMap<>();
    propertiesAppended.put("X", "Y");
    fileToPropertiesArray = new HashMap<>();
    fileToPropertiesArrayAppended = new HashMap<>();
    files = new ArrayList<>();
    filesAppendeds = new ArrayList<>();
    sourceExtensions = Set.of("java", "abcs");
    final Path gensources = path.resolve("generated-sources");

    final Path sgen       = gensources.resolve("main");
    Files.createDirectories(sgen);
    sourcesOutputDirectory = sgen.toFile();
    final Path tgen = gensources.resolve("test");
    Files.createDirectories(tgen);
    testSourcesOutputDirectory = tgen.toFile();
    final Path rod = path.resolve("generated-resources");
    Files.createDirectories(rod);
    resourcesOutputDirectory = rod.toFile();
    final Path trod = path.resolve("generated-test-resources");
    Files.createDirectories(trod);
    testResourcesOutputDirectory = trod.toFile();

    suppliers = new HashMap<>();
    suppliers.put("test1", new DummyTemplatingEngineSupplier());
    suppliers.put("test2", new DummyTemplatingEngineSupplier());
    suppliers.put("throw", new ThrowingDummySupplier());

    // rm = (ResourcesTemplatingEngineMojo) SU(new ResourcesTemplatingEngineMojo());
    // tr = (TestResourcesTemplatingEngineMojo) SU(new
    // TestResourcesTemplatingEngineMojo());
    // sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo());
    // tm = (TestsTemplatingEngineMojo) SU(new TestsTemplatingEngineMojo());
  }

  @Test
  public void testLocalExecute() throws MojoExecutionException, IOException {
    executionIdentifier = "test1";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    templateTestSources = spr.resolve("src").resolve("test").resolve("templates").toFile();
    sm.appendExecutionIdentifierToOutput = true;
    sm.engineHint = engine;
    sm.source = templateSources;
    sm.dumpContext = false;
    sm.propertySuppliers.addAll(ss);
    sm.propertySuppliersMap.putAll(map);

    sm.execute();
  }

  @Test
  public void testLocalExecuteR() throws MojoExecutionException, IOException {
    executionIdentifier = "test1";
    rm = (ResourcesTemplatingEngineMojo) SU(new ResourcesTemplatingEngineMojo(), executionIdentifier);

    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    templateTestSources = spr.resolve("src").resolve("test").resolve("templates").toFile();
    Reflect.on(rm).set("appendExecutionIdentifierToOutput", true).set("engineHint", engine)
        .set("source", templateSources).set("dumpContext", false);

    rm.execute();
  }

  @Test
  public void testLocalExecuteTest2Tests() throws MojoExecutionException, IOException {
    executionIdentifier = "test2";
    tm = (TestsTemplatingEngineMojo) SU(new TestsTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("test").resolve("test-templates").toFile();
    Reflect.on(tm).set("appendExecutionIdentifierToOutput", true).set("engineHint", engine)
        .set("source", templateSources).set("dumpContext", false);

    tm.execute();
  }

  @Test
  public void testLocalExecuteTR() throws MojoExecutionException, IOException {
    executionIdentifier = "test2";
    tr = (TestResourcesTemplatingEngineMojo) SU(new TestResourcesTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("test").resolve("test-templates").toFile();
    Reflect.on(tr).set("appendExecutionIdentifierToOutput", true).set("engineHint", engine)
        .set("source", templateSources).set("dumpContext", false);

    tr.execute();
  }

  @Test // (expected = MojoExecutionException.class)
  public void testLocalExecuteWFileFail() throws MojoExecutionException, IOException {
    executionIdentifier = "test1";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    templateTestSources = spr.resolve("src").resolve("test").resolve("templates").toFile();
    final Map<String, File> fake = new HashMap<>();
    fake.put("someFake", new File(UUID.randomUUID().toString()));
    final List<File> fakeFiles = Arrays.asList(new File(UUID.randomUUID().toString()));
    sm.files.addAll(fakeFiles);
    sm.appendExecutionIdentifierToOutput = false;
    sm.engineHint = engine;
    sm.source = templateSources;
    sm.dumpContext = true;
    sm.fileToPropertiesArrayAppended.putAll(fake);
    sm.execute();
    // TODO Assert Something?
  }
  @Test // (expected = MojoExecutionException.class)
  public void testLocalExecuteWSkip() throws MojoExecutionException, IOException {
    executionIdentifier = "test1";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    templateTestSources = spr.resolve("src").resolve("test").resolve("templates").toFile();
    final Map<String, File> fake = new HashMap<>();
    fake.put("someFake", new File(UUID.randomUUID().toString()));
    final List<File> fakeFiles = Arrays.asList(new File(UUID.randomUUID().toString()));
    sm.files.addAll(fakeFiles);
    sm.appendExecutionIdentifierToOutput = false;
    sm.engineHint = engine;
    sm.source = templateSources;
    sm.dumpContext = true;
    sm.skip = true;
    sm.fileToPropertiesArrayAppended.putAll(fake);
    sm.execute();
    // TODO Assert Something?
  }

  @Test
  public void testLocalExecuteWoAppend() throws MojoExecutionException, IOException {
    executionIdentifier = "test2";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = executionIdentifier;
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateTestSources = spr.resolve("src").resolve("test").resolve("test-templates").toFile();
    Reflect.on(sm).set("appendExecutionIdentifierToOutput", false).set("engineHint", engine)
        .set("source", templateTestSources).set("dumpContext", true);

    sm.execute();

  }

  @Test
  public void testLocalExecuteWThrowingExecution() throws MojoExecutionException, IOException {
    executionIdentifier = "test1";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = "throw";
    appendExecutionIdentifierToOutput = true;
    final Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    templateTestSources = spr.resolve("src").resolve("test").resolve("templates").toFile();
    sm.appendExecutionIdentifierToOutput = false;
    sm.engineHint = engine;
    sm.source = templateSources;
    sm.dumpContext = true;
    assertThrows(MojoExecutionException.class, () -> sm.execute());
  }

  @Test
  public void testWrongEngine() throws IOException {
    executionIdentifier = "test1";
    sm = (SourcesTemplatingEngineMojo) SU(new SourcesTemplatingEngineMojo(), executionIdentifier);
    engine = UUID.randomUUID().toString();
    // appendExecutionIdentifierToOutput = true;
    Path spr = testClasses.resolve(executionIdentifier);
    Files.createDirectories(spr);
    templateSources = spr.resolve("src").resolve("main").resolve("templates").toFile();
    // templateTestSources =
    // spr.resolve("src").resolve("test").resolve("templates").toFile();
    // sourcePathRoot = spr.toFile();
    sm.engineHint = engine;
    sm.source = templateSources;
    assertThrows(MojoExecutionException.class, () -> sm.execute());
  }

  AbstractTemplatingMojo SU(final AbstractTemplatingMojo sm, String execId) {

    TemplatingComponent tc = new TemplatingComponent();

    Logger              l  = new ConsoleLogger(2, "TemplatingEngineMojoTest");
    tc.enableLogging(l);
    final MojoExecution mojo = new MojoExecution(new MojoDescriptor(), execId);
    sm.setComp(tc);
    sm.setLog(new DefaultLog(l));
    sm.suppliers = suppliers;
    sm.properties.putAll(properties);
    sm.mojo = mojo;
    sm.propertiesAppended.putAll(propertiesAppended);
    sm.fileToPropertiesArray.putAll(fileToPropertiesArray);
    sm.fileToPropertiesArrayAppended.putAll(fileToPropertiesArrayAppended);
    sm.files.addAll(files);
    sm.filesAppendeds.addAll(filesAppendeds);
    sm.setSource(templateSources);
    sm.setOutputDirectory(sourcesOutputDirectory);
    sm.sourceExtensions.addAll(sourceExtensions);
    sm.includeDotFiles = includeDotFiles;
    sm.includeHidden = includeHidden;
    sm.includeEnvironment = includeEnvironment;
    sm.includeSystemProperties = includeSystemProperties;
    sm.project = project;

    return sm;
  }
}
