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

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates source code with a TemplatingEngine instace
 *
 */
@Mojo(name = "generate-resources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class ResourcesTemplatingEngineMojo extends AbstractTemplatingMojo {

  @Component(hint = TemplatingComponent.TEMPLATING_COMPONENT)
  public TemplatingComponent comp;

  public void setComp(TemplatingComponent comp) {
    this.comp = comp;
  }

  @Override
  public TemplatingComponent getTemplatingComponent() {
    // TODO Auto-generated method stub
    return comp;
  }

  /**
   * Source folder for templates
   *
   * @required
   */
  @Parameter(required = true, defaultValue = "${basedir}/src/main/resource-templates/")
  public File source;
  /**
   * Output directory for resources.
   *
   */
  @Parameter(required = false, defaultValue = "${project.build.directory}/generated-resources/templating-engine")
  public File outputDirectory;

  @Override
  public void setSource(File source) {
    this.source = source;
  }

  @Override
  public void setOutputDirectory(File out) {
    this.outputDirectory = out;
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
  public TemplateType getType() {
    return TemplateType.RESOURCE;
  }


}
