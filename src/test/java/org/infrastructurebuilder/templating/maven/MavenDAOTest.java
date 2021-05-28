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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.junit.jupiter.api.Test;

class MavenDAOTest extends AbstractPlatformTest {

  @Test
  void testGetId() {
    assertEquals(X, p.getId());
    p = new Platform();
    assertThrows(NullPointerException.class, p::getId);

    PlatformInstance pi = new PlatformInstance();
    assertThrows(NullPointerException.class, pi::getId);
  }

  @Test
  void testAddInstance() {
    p.addInstance(i1);
    List<PlatformInstance> q = p.getInstances();
    assertEquals(1, q.size());
    assertEquals(i1, q.get(0));
  }

  @Test
  void testSetGetPlatform() {
    assertNull(i2.getPlatform());
    i2.setPlatform(p);
    assertEquals(p, i2.getPlatform());
    assertNull(i1.getPlatform());

  }

  @Test
  void testSetInstances() {
    assertEquals(0, p.getInstances().size());
    instances.add(i1);
    p.setInstances(instances);
    assertEquals(instances, p.getInstances());
  }

  @Test
  void testGetDirName() {
    assertEquals(Z, i2.getId());
    assertEquals(Y, i1.getId());
    assertEquals(Y, i1.getDirName());
    assertEquals(DIR, i2.getDirName());
  }

  @Test
  void testSetGetProps() {
    assertThrows(TemplatingEngineException.class, i1::getProperties);
    i1.setProperties(props);
    Properties l0 = i1.getProperties();
    assertEquals(Y, l0.get(X));
    assertThrows(TemplatingEngineException.class, () -> i1.setPath(DIR));
    i2.setPath(DIR);
    Properties lop = i2.getProperties();
    assertEquals("b", lop.getProperty("a"));
    assertEquals("d", lop.getProperty("c"));
    assertEquals(2, lop.size());
  }

  @Test
  void testSetBadPath() {
    i2.setPath(DIR + UUID.randomUUID().toString());
    assertThrows(TemplatingEngineException.class, () -> i2.setProperties(props));
    Path q = i2.getPath().get();
    assertFalse(Files.exists(q));
    assertThrows(TemplatingEngineException.class, () -> i2.getProperties());
  }

  @Test
  void testNameMappers() {
    assertThrows(NullPointerException.class, () -> n1.map(Y));
    assertFalse(n2.map(Y).isPresent());
    assertFalse(n2.map(Z).isPresent());
    assertEquals(Y, n2.map(X).get());
  }

}
