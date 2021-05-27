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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class AbstractPlatformTest {

  private static final TestingPathSupplier tps = new TestingPathSupplier();
  protected static final String Z = "Z";
  protected static final String DIR = tps.getTestClasses().resolve("i2.properties").toString();
  protected static final String Y = "Y";
  protected static final String X = "X";

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
  }

  @AfterAll
  static void afterTearDown() throws Exception {
    tps.finalize();
  }

  protected Platform p;
  protected List<PlatformInstance> instances;
  protected PlatformInstance i1;
  protected PlatformInstance i2;
  protected Properties props;
  protected ClassifierNameMapper n1;
  protected ClassifierNameMapper n2;
  private ClassifierNameMapper n3;

  public AbstractPlatformTest() {
    super();
  }

  @BeforeEach
  void setUp() throws Exception {
    p = new Platform(X);
    instances = new ArrayList<>();
    i1 = new PlatformInstance(Y, null);
    i2 = new PlatformInstance(Z, DIR);
    props = new Properties();
    props.setProperty(X, Y);
    n1 = new ClassifierNameMapper();
    n2 = new ClassifierNameMapper(X, Y);
    n3 = new ClassifierNameMapper(X, null);

  }

  @AfterEach
  void tearDown() throws Exception {
  }

}