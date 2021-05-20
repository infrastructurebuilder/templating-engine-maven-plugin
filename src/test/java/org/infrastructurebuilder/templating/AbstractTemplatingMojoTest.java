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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.infrastructurebuilder.util.core.WorkingPathSupplier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AbstractTemplatingMojoTest {
  private static WorkingPathSupplier wps;
  private static Path target;
  private static Path testClasses;
  private static Path tc;

  @BeforeClass
  public static void setupClass() {
    wps = new WorkingPathSupplier();
    target = wps.getRoot();
    testClasses = target.resolve("test-classes");
    tc = testClasses.resolve("test2");
  }

  private final Map<String, File> array = new HashMap<>();
  private final Map<String, File> appended = new HashMap<>();

  @Before
  public void setUp() throws Exception {
    final Path f1 = tc.resolve("file1.txt");
    final Path a1 = tc.resolve("file2.txt");
    array.put("1", f1.toFile());
    appended.put("2", a1.toFile());
    appended.put("1", a1.toFile());
  }

  @Test //(expected = MojoExecutionException.class)
  public void testFailGenerateFileToPropertiesArray() throws MojoExecutionException {
    final File f4 = tc.resolve("file5.properties").toFile();
    appended.put("3", f4);
    Map<String, Object>  b = AbstractTemplatingMojo.generateFileToPropertiesArray(array, appended);
    assertEquals(b.size(), 2);
  }

  @Test
  public void testFailGetFilesProperties() throws TemplatingEngineException {
    final File f3 = tc.resolve("file3.properties").toFile();
    final File f4 = tc.resolve("file5.properties").toFile();
    final Map<String, Object> kk = AbstractTemplatingMojo.getFilesProperties(Arrays.asList(f3), Arrays.asList(f4));
    assertEquals(2, kk.size());
  }

  @Test
  public void testGenerateFileToPropertiesArray() throws MojoExecutionException {
    final Map<String, Object> b = AbstractTemplatingMojo.generateFileToPropertiesArray(array, appended);
    assertEquals(b.size(), 2);
  }

  @Test
  public void testGetFilesProperties() throws MojoExecutionException, TemplatingEngineException {
    final File f3 = tc.resolve("file3.properties").toFile();
    final File f4 = tc.resolve("file4.properties").toFile();
    final Map<String, Object> kk = AbstractTemplatingMojo.getFilesProperties(Arrays.asList(f3), Arrays.asList(f4));
    assertEquals(3, kk.size());
  }

}
