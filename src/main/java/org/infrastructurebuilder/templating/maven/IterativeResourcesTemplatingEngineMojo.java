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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Stack;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.infrastructurebuilder.templating.maven.internal.InternalPlatform;
import org.infrastructurebuilder.templating.maven.internal.IterativeTemplatingComponent;
import org.infrastructurebuilder.templating.maven.internal.TemplatingComponent;

@Mojo(name = "iterate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true)
public class IterativeResourcesTemplatingEngineMojo extends AbstractTemplatingMojo {
  /**
   * ORDERED List of sub-platform types for platform-array-generation
   * Each sub platform will generated a pathed output from the root and all previous sub platforms
   *
   */
  @Parameter(required = true, defaultValue = "")
  public List<Platform>               platforms      = new ArrayList<>();

  @Parameter(required = false, defaultValue = "true")
  public boolean                      addIdentifiers;

  /**
   * finalOverrides are applied LAST.  If anything needs to be over-ridden, this is the place to do it
   */
  @Parameter(required = false)
  public Properties                   finalOverrides = new Properties();

  @Parameter(required = false)
  public File                         pathPropertiesRoot;

  @Parameter(required = false, defaultValue = "path.properties")
  public String                       pathPropertiesName;
  /**
   * By default (and for convenience), we use the location of dependency:unpack-dependencies
   *
   * @required
   */
  @Parameter(required = true, defaultValue = "${project.build.directory}/dependency")
  public File                         source;
  /**
   * Output directory for resources.
   *
   */
  @Parameter(required = false, defaultValue = "${project.build.directory}/generated-resources/iterated-templating-resources")
  public File                         outputDirectory;

  @Component
  public IterativeTemplatingComponent icomp;

  @Override
  public TemplatingComponent setup() throws MojoExecutionException {
    IterativeTemplatingComponent c = (IterativeTemplatingComponent) super.setup();
    c.outputDirectory = outputDirectory.toPath();
    c.finalOverrides = finalOverrides;
    c.addIdentifiers = addIdentifiers;
    c.pathPropertiesRoot = Optional.ofNullable(pathPropertiesRoot).map(File::toPath).map(Path::toAbsolutePath)
        .orElse(null);
    c.pathPropertiesName = pathPropertiesName;
    if (platforms.size() < 1)
      throw new MojoExecutionException("At least one <platform> must be specified");
    List<InternalPlatform> ipl   = new ArrayList<>(
        // List of InternalPlatform instances based solely off the first platform
        platforms.get(0).getInstances().stream().map(pi -> new InternalPlatform(platforms.get(0)).extend(pi))
            .collect(toList()));
    List<Platform>         plist = new ArrayList<>();
    plist.addAll(platforms.subList(1, platforms.size()));
    Stack<Platform> remainingPlatforms = new Stack<>();
    remainingPlatforms.addAll(plist);
    while (remainingPlatforms.size() > 0) {
      List<InternalPlatform> newIpl = new ArrayList<>();
      Platform               head   = remainingPlatforms.pop();
      List<InternalPlatform> wl     = ipl.stream().map(InternalPlatform::copy).collect(toList());
      head.getInstances().forEach(pi -> {
        List<InternalPlatform> l = wl.stream().map(item -> item.extend(pi)).collect(toList());
        newIpl.addAll(l);
      });
      ipl = newIpl;
    }
    c.internalPlatformList = ipl;

    return c;
  }

  @Override
  public void execute() throws MojoExecutionException {

    if (!skip) {
      @SuppressWarnings("rawtypes")
      Map pc = getPluginContext();

      setup().execute(pc);

      setPluginContext(pc);

    } else

    {
      getLog().info("Skipping templating for " + mojo.getExecutionId());
    }
  }

  @Override
  public File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  public Path getScanningRootSource() {
    return source.toPath();
  }

  @Override
  public void setSource(File source) {
    this.source = source;
  }

  @Override
  public void setOutputDirectory(File out) {
    this.outputDirectory = out;
  }

  @Override
  public TemplateType getType() {
    return TemplateType.ITERATIVE_RESOURCE;
  }

  @Override
  public void setComp(TemplatingComponent comp) {
    this.icomp = (IterativeTemplatingComponent) comp;
  }
  @Override
  public TemplatingComponent getTemplatingComponent() {
    return icomp;
  }

}
