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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.maven.AbstractPlatformTest;
import org.infrastructurebuilder.templating.maven.InternalPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InternalPlatformTest extends AbstractPlatformTest {

  private InternalPlatform ipl1, ipl2, ipl3;

  @BeforeEach
  void beforeEach() {
    ipl1 = new InternalPlatform(p).copy();
  }

  @Test
  void testExtend() {
    String k = ipl1.getIdsJoinedDashString();
    assertEquals("",ipl1.getInstancePlatformIdsJoinedDot());
    assertEquals("", k);

    i1.setProperties(new Properties());
    assertEquals("InternalPlatform [paths=, tp={}, root=X]",ipl1.toString());
    i2.setPath(DIR);

    ipl2 = ipl1.extend(i1).copy();
    k = ipl2.getIdsJoinedDashString();
    assertEquals(Y, k);
    ipl3 = ipl2.extend(i2);
    k = ipl3.getIdsJoinedDotString();
    assertEquals(Y + "." + Z, k);
    assertEquals(Y + "_" + Z, ipl3.getIdsJoinedUSString());
    String a = ipl3.getPathJoinedUSString();
    assertEquals(Y + "_" + DIR, a);
    assertEquals(Y + "-" + DIR, ipl3.getPathJoinedDashString());
    assertEquals(Y + "." + DIR, ipl3.getPathJoinedDotString());

    String paths = ipl3.getPaths();
    assertTrue(paths.contains(FileSystems.getDefault().getSeparator()));

    Path q = tps.get();

    MSOSupplier v = ipl3.getMSO(true);
    Map<String, Object> ms = v.get();
    assertEquals(10,ms.size());



  }

  @Test
  void testGetExtendedPath() {
//    fail("Not yet implemented");
  }

  @Test
  void testGetFinalDestination() {
    Path fd = tps.get();
    assertFalse(ipl1.getFinalDestination().isPresent());
    ipl1.setFinalDestination(fd);
    Path q = ipl1.getFinalDestination().get();
    assertEquals(fd, q);
  }

}
