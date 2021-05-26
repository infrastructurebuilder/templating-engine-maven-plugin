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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Platform layers properties atop each other and extends the target name by the value of the id
 *
 * So platform "X" with props "X.properties"
 * @author mykel
 *
 */
public class Platform {
  private String id;
  private List<PlatformInstance> instances = new ArrayList<>();

  public String getId() {
    return requireNonNull(id, "id must contain a value");
  }

  public List<PlatformInstance> getInstances() {
    return instances;
  }

  public void addInstance(PlatformInstance i) {
    this.instances.add(i);
    i.setPlatform(this);
  }

  public void setInstances(List<PlatformInstance> instances) {
    instances.forEach(this::addInstance);
  }


}
