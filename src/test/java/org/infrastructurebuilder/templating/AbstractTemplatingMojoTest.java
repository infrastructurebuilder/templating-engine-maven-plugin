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


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.infrastructurebuilder.templating.maven.TemplatingUtils;
import org.infrastructurebuilder.util.core.WorkingPathSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractTemplatingMojoTest {
  private static WorkingPathSupplier wps;
  private static Path                target;
  private static Path                testClasses;
  private static Path                tc;

  @BeforeAll
  public static void setupClass() {
    wps = new WorkingPathSupplier();
    target = wps.getRoot();
    testClasses = target.resolve("test-classes");
    tc = testClasses.resolve("test2");
  }

  private final Map<String, Path> array    = new HashMap<>();
  private final Map<String, Path> appended = new HashMap<>();

  @BeforeEach
  public void setUp() throws Exception {
    final Path f1 = tc.resolve("file1.txt");
    final Path a1 = tc.resolve("file2.txt");
    array.put("1", f1);
    appended.put("2", a1);
    appended.put("1", a1);
  }

  @Test // (expected = MojoExecutionException.class)
  public void testFailGenerateFileToPropertiesArray() throws MojoExecutionException {
    final Path f4 = tc.resolve("file5.properties");
    appended.put("3", f4);
    Map<String, Object> b = TemplatingUtils.generateFileToPropertiesArray(array, appended);
    assertEquals(b.size(), 2);
  }

  @Test
  public void testFailGetFilesProperties() throws TemplatingEngineException {
    final Path                f3 = tc.resolve("file3.properties");
    final Path                f5 = tc.resolve("file5.properties");
    final Map<String, Object> kk = TemplatingUtils.getFilesProperties(Arrays.asList(f3, f5));
    assertEquals(2, kk.size());
  }

  @Test
  public void testGenerateFileToPropertiesArray() throws MojoExecutionException {
    final Map<String, Object> b = TemplatingUtils.generateFileToPropertiesArray(array, appended);
    assertEquals(b.size(), 2);
  }

  @Test
  public void testGetFilesProperties() throws MojoExecutionException, TemplatingEngineException {
    final Path                f3 = tc.resolve("file3.properties");
    final Path                f4 = tc.resolve("file4.properties");
    final Map<String, Object> kk = TemplatingUtils.getFilesProperties(Arrays.asList(f3, f4));
    assertEquals(3, kk.size());
  }

}
