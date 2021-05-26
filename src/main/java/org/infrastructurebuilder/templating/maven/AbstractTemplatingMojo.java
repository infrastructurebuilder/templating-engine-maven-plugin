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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.TemplatingEngineSupplier;
import org.infrastructurebuilder.templating.maven.internal.TemplatingUtils;

public abstract class AbstractTemplatingMojo extends AbstractMojo {

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  protected MojoExecution                                   mojo;

  @Parameter(required = false)
  protected boolean                               appendExecutionIdentifierToOutput;

  @Parameter(required = false)
  protected boolean                               dumpContext;

  @Parameter(required = false, defaultValue = "false")
  protected boolean                               skip;

  /**
   * PropertySuppliers are the list that injects which of the
   * PropertiesSupplier components are injected into the
   * final Properties and in what order
   */
  @Parameter
  protected final List<String>                    propertySuppliers             = new ArrayList<>();

  @Component
  protected final Map<String, MSOSupplier>        propSuppliers                 = new HashMap<>();

  /**
   * Extra properties
   *
   */
  @Parameter(required = false)
  protected final Map<String, String>               properties                    = new HashMap<>();

  /**
   * Extra properties added to "properties" , allowing us to use properties
   * as a base and add per-execution
   *
   */

  @Parameter(required = false)
  protected final Map<String, String>               propertiesAppended            = new HashMap<>();

  /**
   * Reads a file of lines, setting a property equal to the key that maps to a
   * JSONArray of quoted strings from lines in the file.
   *
   * Thus <A>file.txt</A>
   *
   * where file.txt is A B C Would add the property A= ["A","B","C"]
   *
   * Lines beginning with "#" or "//" are ignored as comments Blank lines are not
   * allowed
   *
   */
  @Parameter(required = false)
  protected final Map<String, File>                 fileToPropertiesArray         = new HashMap<>();

  /**
   * Mappings added to the property above. This allows us to use
   * filesToPropertiesArray as a configured base and then add per-execution
   */
  @Parameter(required = false)
  protected final Map<String, File>                 fileToPropertiesArrayAppended = new HashMap<>();

  /**
   * Files, in order, to read in and merge to make a single Properties object that
   * gets added to the existing properties
   *
   * Later files over-ride earlier files in this list, as the Properties created
   * uses each item as the "defaults" for each previous item.
   *
   */
  @Parameter(required = false)
  protected final List<File>                        files                         = new ArrayList<>();
  /**
   * Files appended to the "files" parameter from above. This allows us to use
   * "files" as a base config and then add values to it per-execution
   */
  @Parameter(required = false)
  protected final List<File>                      filesAppendeds                = new ArrayList<>();

  /**
   * List of @{code sourceExtension} elements that are "source" files Defaults to
   * ["java", "scala", "groovy", "clj"]
   */
  @Parameter(required = false)
  protected final Set<String>                     sourceExtensions              = null;

  /**
   * Include files with names beginning with "."
   */
  @Parameter(required = true, defaultValue = "false")
  protected boolean                               includeDotFiles;

  /**
   * Include files marked as "hidden" by the OS
   */
  @Parameter(required = true, defaultValue = "false")
  protected boolean                               includeHidden;

  /**
   * Include System.getProperties as a starter element for the properties
   */
  @Parameter(required = true, defaultValue = "false")
  protected boolean                               includeSystemProperties;
  /**
   * Include System.getenv() in properties
   */
  @Parameter(required = true, defaultValue = "false")
  protected boolean                               includeEnvironment;

  @Parameter(required = true, defaultValue = "false")
  protected boolean                               caseSensitive;

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  protected MavenProject                          project;

  @Parameter(required = true)
  protected String                                engineHint;

  @Parameter(required = false, defaultValue = "true")
  protected boolean                               useSourceParent               = true;

  /**
   * This map is automatically populated from the dependency tree via plexus
   */
  @Component(role = TemplatingEngineSupplier.class)
  protected Map<String, TemplatingEngineSupplier> suppliers;

  @Override
  public void execute() throws MojoExecutionException {
    if (!skip) {
      Path parentPath = getScanningRootSource().toPath();
      if (useSourceParent)
        parentPath = parentPath.getParent();
      TemplatingUtils.localExecute(getType(), mojo.getExecutionId(), appendExecutionIdentifierToOutput, suppliers,
          engineHint,TemplatingUtils.mapSS2Props.apply(properties), TemplatingUtils.mapSS2Props.apply(propertiesAppended), fileToPropertiesArray, fileToPropertiesArrayAppended,
          files, filesAppendeds, parentPath, getScanningRootSource(), getOutputDirectory().toPath(), sourceExtensions,
          includeDotFiles, includeHidden, dumpContext, includeSystemProperties, includeEnvironment,
          requireNonNull(project, "maven project"), getLog(), caseSensitive, propertySuppliers, propSuppliers);
    } else {
      getLog().info("Skipping templating for " + mojo.getExecutionId());
    }
  }

  abstract public TemplateType getType();

  protected abstract File getOutputDirectory();

  protected abstract File getScanningRootSource();

}
