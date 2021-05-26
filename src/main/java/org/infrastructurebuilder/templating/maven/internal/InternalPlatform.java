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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.maven.Platform;
import org.infrastructurebuilder.templating.maven.PlatformInstance;

public class InternalPlatform {
  private List<String>           ids       = new ArrayList<>();
  private List<String>           paths     = new ArrayList<>();
  private List<Properties>       tp        = new ArrayList<>();
  private List<PlatformInstance> instances = new ArrayList<>();
  private Platform               root;
  private Path                   finalDestination;

  public InternalPlatform(Platform root) {
    this.root = requireNonNull(root);
  }

  private InternalPlatform(InternalPlatform i) {
    this.instances.addAll(i.instances);
    this.ids.addAll(i.ids);
    this.paths.addAll(i.paths);
    this.tp.addAll(i.tp);
    this.root = i.root;
  }

  public InternalPlatform extend(PlatformInstance pi) {
    InternalPlatform i = copy();
    i.ids.add(requireNonNull(pi, "Platform Instance" + PlatformInstance.NON_NULL).getId());
    i.paths.add(pi.getDirName());
    i.tp.add(pi.getProperties());
    i.instances.add(pi);
    return i;
  }

  public String getIdsJoinedDashString() {
    return String.join("-", ids);
  }

  public String getIdsJoinedDotString() {
    return String.join(".", ids);
  }

  public String getIdsJoinedUSString() {
    return String.join("_", ids);
  }

  public String getPathJoinedDashString() {
    return String.join("-", paths);
  }

  public String getPathJoinedDotString() {
    return String.join(".", paths);
  }

  public String getPathJoinedUSString() {
    return String.join("_", paths);
  }

  public String getInstancePlatformIdsJoinedDot() {
    return this.instances.stream().map(PlatformInstance::getPlatform).map(Platform::getId).collect(joining("."));
  }

  public String getPaths() {
    return String.join(FileSystems.getDefault().getSeparator(), paths);
  }

  public Path getExtendedPath(Path root) {
    Path c = requireNonNull(root);
    for (int i = 0; i < paths.size(); ++i)
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
    return "InternalPlatform [paths=" + getPaths() + ", tp=" + getProperties() + ", root=" + root.getId() + "]";
  }

  public MSOSupplier getMSO(boolean addProperties) {
    Properties p = getProperties();
    if (addProperties) {
      p.setProperty("ids_joined_dash", getIdsJoinedDashString());
      p.setProperty("ids_joined_dot", getIdsJoinedDotString());
      p.setProperty("ids_joined_under", getIdsJoinedUSString());
      p.setProperty("path_joined_dash", getPathJoinedDashString());
      p.setProperty("path_joined_dot", getPathJoinedDotString());
      p.setProperty("path_joined_under", getPathJoinedUSString());
      p.setProperty("paths_joined_sep", getPaths());
      p.setProperty("final_destination", getFinalDestination().map(Path::toString).orElse(":unknown:"));
    }
    Map<String, Object> v = TemplatingUtils.toMSO.apply(p);
    return () -> v;
  }

  public void setFinalDestination(Path finalDestination) {
    this.finalDestination = requireNonNull(finalDestination).toAbsolutePath();
  }

  public Optional<Path> getFinalDestination() {
    return Optional.ofNullable(this.finalDestination);
  }
}
