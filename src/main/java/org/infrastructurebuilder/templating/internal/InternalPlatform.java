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
package org.infrastructurebuilder.templating.internal;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.infrastructurebuilder.templating.AbstractTemplatingMojo;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.Platform;
import org.infrastructurebuilder.templating.PlatformInstance;

public class InternalPlatform implements MSOSupplier {
  private List<String>     paths = new ArrayList<>();
  private List<Properties> tp    = new ArrayList<>();
  private Platform         root;

  public InternalPlatform(Platform root) {
    this.root = Objects.requireNonNull(root);
  }

  private InternalPlatform(InternalPlatform i) {
    this.paths.addAll(i.paths);
    this.tp.addAll(i.tp);
    this.root = i.root;
  }

  public InternalPlatform extend(PlatformInstance pi) {
    InternalPlatform i = copy();
    i.paths.add(Objects.requireNonNull(pi.getId()));
    i.tp.add(Objects.requireNonNull(pi.getProperties()));
    return i;
  }

  public String getRoot() {
    return String.join(FileSystems.getDefault().getSeparator(), paths);
  }

  public Path getExtendedPath(Path root) {
    Path c = Objects.requireNonNull(root);
    for (int i= 0; i < paths.size(); ++i)
      c = c.resolve(paths.get(i));
    return c;
  }

  public Properties getProperties() {
    Properties p = new Properties();
    tp.forEach(p::putAll);
    return p;
  }

  public InternalPlatform copy() {
    return new InternalPlatform(this);
  }

  @Override
  public String toString() {
    return "InternalPlatform [paths=" + getRoot() + ", tp=" + getProperties() + ", root=" + root.getId() + "]";
  }

  @Override
  public Map<String, Object> get() {
    return AbstractTemplatingMojo.toMSO.apply(getProperties());
  }

}
