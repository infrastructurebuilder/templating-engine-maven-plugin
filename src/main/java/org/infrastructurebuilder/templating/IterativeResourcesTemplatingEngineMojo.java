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

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates source code with a TemplatingEngine instace
 *
 */
@Mojo(name = "iterate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true)
public class IterativeResourcesTemplatingEngineMojo extends AbstractIterativeTemplatingMojo {

  /**
   * By default (and for convenience), we use the location of dependency:unpack-dependencies
   *
   * @required
   */
  @Parameter(required = true, defaultValue = "${project.build.directory}/dependency")
  private File source;
  /**
   * Output directory for resources.
   *
   */
  @Parameter(required = false, defaultValue = "${project.build.directory}/generated-resources/iterated-templating-resources")
  private File outputDirectory;

  @Override
  public File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  public File getScanningRootSource() {
    return source;
  }

  @Override
  public TemplateType getType() {
    return TemplateType.ITERATIVE_RESOURCE;
  }


}