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
package org.infrastructurebuilder.templating.maven.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IterativeTemplatingComponentTest {
  private static final TestingPathSupplier tps    = new TestingPathSupplier();
  private static final Logger              logger = new ConsoleLogger(2,
      IterativeTemplatingComponentTest.class.getName());
  protected static final String            Z      = "Z";
  protected static final String            DIR    = tps.getTestClasses().resolve("i2.properties").toString();

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {
    tps.finalize();
  }

  private IterativeTemplatingComponent ic;

  @BeforeEach
  void setUp() throws Exception {
    ic = new IterativeTemplatingComponent();
  }

  @AfterEach
  void tearDown() throws Exception {
  }

  @Test
  void testExecute() {
    assertThrows(Throwable.class, () -> ic.execute(new HashMap<>()));
  }

  @Test
  void testGetLog() {
    assertNull(ic.getLog());
    ic.enableLogging(logger);
    assertEquals(logger, ic.getLog());
  }

}
