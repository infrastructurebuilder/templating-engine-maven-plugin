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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public abstract class AbstractTemplatingMojo extends AbstractMojo {

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  public MojoExecution                         mojo;

  @Parameter(required = false)
  public boolean                               appendExecutionIdentifierToOutput;

  @Parameter(required = false)
  public boolean                               dumpContext;

  @Parameter(required = false, defaultValue = "false")
  public boolean                               skip;

  /**
   * PropertySuppliers are the list that injects which of the
   * PropertiesSupplier components are injected into the
   * final Properties and in what order
   */
  @Parameter
  public final List<String>                    propertySuppliers             = new ArrayList<>();

  @Component
  public final Map<String, MSOSupplier>        propertySuppliersMap          = new HashMap<>();

  /**
   * Extra properties
   *
   */
  @Parameter(required = false)
  public final Map<String, String>             properties                    = new HashMap<>();

  /**
   * Extra properties added to "properties" , allowing us to use properties
   * as a base and add per-execution
   *
   */

  @Parameter(required = false)
  public final Map<String, String>             propertiesAppended            = new HashMap<>();

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
  public final Map<String, File>               fileToPropertiesArray         = new HashMap<>();

  /**
   * Mappings added to the property above. This allows us to use
   * filesToPropertiesArray as a configured base and then add per-execution
   */
  @Parameter(required = false)
  public final Map<String, File>               fileToPropertiesArrayAppended = new HashMap<>();

  /**
   * Files, in order, to read in and merge to make a single Properties object that
   * gets added to the existing properties
   *
   * Later files over-ride earlier files in this list, as the Properties created
   * uses each item as the "defaults" for each previous item.
   *
   */
  @Parameter(required = false)
  public final List<File>                      files                         = new ArrayList<>();
  /**
   * Files appended to the "files" parameter from above. This allows us to use
   * "files" as a base config and then add values to it per-execution
   */
  @Parameter(required = false)
  public final List<File>                      filesAppendeds                = new ArrayList<>();

  /**
   * List of @{code sourceExtension} elements that are "source" files Defaults to
   * ["java", "scala", "groovy", "clj"]
   */
  @Parameter(required = false)
  public final Set<String>                     sourceExtensions              = new HashSet<>();

  /**
   * Include files with names beginning with "."
   */
  @Parameter(required = true, defaultValue = "false")
  public boolean                               includeDotFiles;

  /**
   * Include files marked as "hidden" by the OS
   */
  @Parameter(required = true, defaultValue = "false")
  public boolean                               includeHidden;

  /**
   * Include System.getProperties as a starter element for the properties
   */
  @Parameter(required = true, defaultValue = "false")
  public boolean                               includeSystemProperties;
  /**
   * Include System.getenv() in properties
   */
  @Parameter(required = true, defaultValue = "false")
  public boolean                               includeEnvironment;

  @Parameter(required = true, defaultValue = "false")
  public boolean                               caseSensitive;

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  public MavenProject                          project;

  @Parameter(required = true)
  public String                                engineHint;

  @Parameter(required = false, defaultValue = "true")
  public boolean                               useSourceParent               = true;

  @Component(hint = TemplatingComponent.TEMPLATING_COMPONENT)
  public TemplatingComponent comp;

  public TemplatingComponent getTemplatingComponent() {
    return comp;
  }

  public void setComp(TemplatingComponent comp) {
    this.comp = comp;
  }

  /**
   * This map is automatically populated from the dependency tree via plexus
   */
  @Component(role = TemplatingEngineSupplier.class)
  public Map<String, TemplatingEngineSupplier> suppliers;

  @Override
  public void execute() throws MojoExecutionException {
    if (!skip) {
      setup().execute(getPluginContext());
    } else {
      getLog().info("Skipping templating for " + mojo.getExecutionId());
    }
  }

  public TemplatingComponent setup() throws MojoExecutionException {
    TemplatingComponent c = getTemplatingComponent();
    c.type = getType();
    c.executionId = requireNonNull(mojo).getExecutionId();
    c.properties = properties.entrySet().stream().collect(toMap(k -> k.getKey(), v -> v.getValue()));
    c.propertiesAppended = propertiesAppended.entrySet().stream().collect(toMap(k -> k.getKey(), v -> v.getValue()));
    c.fileToPropertiesArray = fileToPropertiesArray.entrySet().stream()
        .collect(toMap(k -> k.getKey(), v -> v.getValue().toPath().toAbsolutePath()));
    c.fileToPropertiesArrayAppended = fileToPropertiesArrayAppended.entrySet().stream()
        .collect(toMap(k -> k.getKey(), v -> v.getValue().toPath().toAbsolutePath()));
    ;
    c.project = requireNonNull(this.project);
    c.sourceExtensions = sourceExtensions;
    c.useSourceParent = useSourceParent;
    c.systemProperties = TemplatingUtils.toMSO
        .apply(includeSystemProperties ? System.getProperties() : new Properties());
    c.env = includeEnvironment ? new HashMap<>() : new HashMap<>(System.getenv());

    ArrayList<Path> f = new ArrayList<>(files.stream().map(File::toPath).collect(toList()));
    f.addAll(filesAppendeds.stream().map(File::toPath).collect(toList()));
    c.files = f;
    c.appendExecutionIdentifierToOutput = appendExecutionIdentifierToOutput;
    c.propertySupplierKeys = propertySuppliers;
    c.propertySupplierMap = propertySuppliersMap;
    c.scanningRootSource = getScanningRootSource();
    c.outputDirectory = getOutputDirectory().toPath();

    TemplatingEngineSupplier comp = ofNullable(suppliers.get(engineHint))
        .orElseThrow(() -> new MojoExecutionException("No engineHint supplier named '" + engineHint + "'"));
    comp.setLog(getLog());
    comp.setProject(project);
    comp.setIncludeDotFiles(includeDotFiles);
    comp.setIncludeHiddenFiles(includeHidden);
    comp.setSourceExtensions(sourceExtensions);
    comp.setCaseSensitive(caseSensitive);

    c.templatingEngineSupplier = comp;
    return c;
  }

  abstract public TemplateType getType();

  protected abstract File getOutputDirectory();

  protected abstract Path getScanningRootSource();

  public abstract void setSource(File source);

  public abstract void setOutputDirectory(File out);

}
