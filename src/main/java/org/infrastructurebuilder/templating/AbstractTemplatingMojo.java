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

import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.templating.TemplatingEngine.EXECUTION_IDENTIFIER;
import static org.infrastructurebuilder.templating.TemplatingEngine.mergeProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.util.core.IBUtils;
import org.json.JSONArray;

public abstract class AbstractTemplatingMojo extends AbstractMojo {

  public final static Function<Map<String, String>, Properties> mapSS2Props = (mss) -> {
    final Properties p = new Properties();
    mss.entrySet().stream().forEach(e -> p.setProperty(e.getKey(), e.getValue()));
    return p;
  };

  public final static Map<String, Object> extendWithAll(Map<String, JSONArray> intial, Map<String, JSONArray> extend) {
    final Map<String, JSONArray> map = new HashMap<>();
    map.putAll(requireNonNull(intial));
    for (Entry<String, JSONArray> e : requireNonNull(extend).entrySet()) {
      if (map.containsKey(e.getKey())) {
        IBUtils.asStream(e.getValue()).forEach(val -> map.get(e.getKey()).put(val));
      } else
        map.put(e.getKey(), e.getValue());
    }
    return map.entrySet().stream().collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()));
  }

  public final static Function<Map<String, File>, Map<String, JSONArray>> _getSingleMap = (m) -> {
    final Map<String, JSONArray> map = new HashMap<>();
    requireNonNull(m).entrySet().forEach(e -> {
      if (e.getValue().isFile() && e.getValue().canRead()) {
        try {
          final JSONArray arry = new JSONArray();
          Files.readAllLines(e.getValue().toPath())
              // Now stream
              .stream()
              // And trim
              .map(TemplatingEngine::trimToString)
              // And filter comments
              .filter(TemplatingEngine::noComment).forEach(ee -> arry.put(ee));
          map.put(
              // Property key
              e.getKey(),
              // New JSONArray.toString()
              arry);
        } catch (final IOException e1) {
          throw new TemplatingEngineException("Failed to read " + e.getValue().getAbsolutePath(), e1);
        }
      }
    });
    return map;
  };

  public static Map<String, Object> generateFileToPropertiesArray(final Map<String, File> array,
      final Map<String, File> appended) throws MojoExecutionException {
    return extendWithAll(_getSingleMap.apply(array), _getSingleMap.apply(appended));
  }

  public final static Function<Properties, Map<String, Object>> toMSO = (p) -> {
    return requireNonNull(p).stringPropertyNames().stream()
        .collect(Collectors.toMap(Function.identity(), v -> p.getProperty(v)));
  };

  public static Map<String, Object> getFilesProperties(final List<File> filez, final List<File> appended)
      throws TemplatingEngineException {
    Map<String, Object> p = new HashMap<>();
    final List<File> workingFiles = new ArrayList<>();
    workingFiles.addAll(filez);
    workingFiles.addAll(appended);
    for (final File f : workingFiles) {
      if (f.isFile() && f.canRead()) {
        try (InputStream ins = Files.newInputStream(f.toPath())) {
          final Properties temp = new Properties();
          temp.load(ins);
          p = mergeProperties(p, toMSO.apply(temp));
        } catch (final IOException e) {
          throw new TemplatingEngineException("Failed to load " + f, e);
        }
      }
    }
    return p;
  }

  /**
   * Send every parameter to a static function that is easily testable without having to
   * actually start up a [full] Maven context.  It still requires a Maven project though
   *
   * @param executionIdentifier
   * @param appendExecutionIdentifierToOutput
   * @param suppliers
   * @param engineHint
   * @param properties
   * @param propertiesAppended
   * @param fileToPropertiesArray
   * @param fileToPropertiesArrayAppended
   * @param files
   * @param filesAppendeds
   * @param sourcePathRoot
   * @param templateSources
   * @param sourcesOutputDirectory
   * @param resourcesOutputDirectory
   * @param sourceExtensions
   * @param includeDotFiles
   * @param includeHidden
   * @param dumpContext
   * @param project
   * @param log
   * @param caseSensitive
   * @param list
   * @param map
   * @throws MojoExecutionException
   */
  public final static void localExecute(final TemplateType type, final String executionIdentifier,
      final boolean appendExecutionIdentifierToOutput, final Map<String, TemplatingEngineSupplier> suppliers,
      final String engineHint, final Properties properties, final Properties propertiesAppended,
      final Map<String, File> fileToPropertiesArray, final Map<String, File> fileToPropertiesArrayAppended,
      final List<File> files, final List<File> filesAppendeds, final Path sourcePathRoot, final File templateSources,
      Path sourcesOutputDirectory, final Set<String> sourceExtensions, final boolean includeDotFiles,
      final boolean includeHidden, final boolean dumpContext, final MavenProject optProject, final Log log,
      final boolean caseSensitive, final List<String> list, final Map<String, MSOSupplier> map)
      throws MojoExecutionException {
    TemplatingEngineSupplier comp;

    comp = Optional.ofNullable(suppliers.get(engineHint))
        .orElseThrow(() -> new MojoExecutionException("No engineHint supplier named '" + engineHint + "'"));

    Map<String, Object> real;
    try {
      real = mergeProperties(
          // Get all properties from files and filesAppendeds
          getFilesProperties(files, filesAppendeds),
          // Merged with all array properties and appendeds
          extendWithAll(_getSingleMap.apply(fileToPropertiesArray), _getSingleMap.apply(fileToPropertiesArrayAppended)),
          // And finall all the properties
          toMSO.apply(properties),
          // And then all appended properties
          toMSO.apply(propertiesAppended));

      // THEN we use the suppliers
      for (final String pval : list) { // Some suppliers need addl configuration
        final MSOSupplier pv = requireNonNull(map.get(pval), "Properties supplier " + pval + " not found");
        if (pv instanceof AbstractMavenBackedPropertiesSupplier) {
          ((AbstractMavenBackedPropertiesSupplier) pv)
              // Inject Maven Project
              .setMavenProject(optProject)
              // Inject current properties
              .setCurrentPropertiesValues(real);
        }
        real.putAll(pv.get());
      }
      if (real.containsKey(EXECUTION_IDENTIFIER)) {
        log.warn("Execution will overwrite " + EXECUTION_IDENTIFIER + "=" + real.get(EXECUTION_IDENTIFIER));
      }
      // Overwrites any other property
      real.put(EXECUTION_IDENTIFIER, executionIdentifier);
    } catch (final TemplatingEngineException e1) {
      throw new MojoExecutionException("Failed to read properties", e1);
    }
    if (appendExecutionIdentifierToOutput) {
      sourcesOutputDirectory = sourcesOutputDirectory.resolve(executionIdentifier);
    }

    comp.setLog(log);
    comp.setProject(optProject);
    comp.setProperties(real);
    comp.setSourcesOutputDirectory(sourcesOutputDirectory);
    comp.setIncludeDotFiles(includeDotFiles);
    comp.setIncludeHiddenFiles(includeHidden);
    comp.setSourceExtensions(sourceExtensions);
    comp.setCaseSensitive(caseSensitive);
    try {
      comp.setSourcePathRoot(sourcePathRoot);
      comp.setExecutionSource(templateSources.toPath());
      final Optional<String> s = comp.get().execute();
      if (s.isPresent()) {
        if (dumpContext) {
          log.info("Context for main execution is [probably]: \n" + s.get());
        }
        final Resource res = new Resource();
        res.setDirectory(sourcesOutputDirectory.toAbsolutePath().toString());
        final String finalSource = sourcesOutputDirectory.toString();
        switch (type) {
        case SOURCE:
          optProject.addCompileSourceRoot(finalSource);
          break;
        case TEST_SOURCE: // FIXME source output wrong!
          optProject.addTestCompileSourceRoot(finalSource);
          break;
        case RESOURCE:
          optProject.addResource(res);
          break;
        case TEST_RESOURCE:
          optProject.addTestResource(res);
          break;
        }
      }
    } catch (final Exception e) {
      throw new MojoExecutionException("Failed to execute TemplatingEngine", e);
    }

  }

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  MojoExecution mojo;

  @Parameter(required = false)
  boolean appendExecutionIdentifierToOutput;

  @Parameter(required = false)
  private boolean dumpContext;

  @Parameter(required = false, defaultValue = "false")
  private boolean skip;

  /**
   * PropertySuppliers are the list that injects which of the
   * PropertiesSupplier components are injected into the
   * final Properties and in what order
   */
  @Parameter
  private final List<String> propertySuppliers = new ArrayList<>();

  @Component
  private final Map<String, MSOSupplier> propSuppliers = new HashMap<>();

  /**
   * Extra properties
   *
   */
  @Parameter(required = false)
  private final Map<String, String> properties = new HashMap<>();

  /**
   * Extra properties added to "properties" , allowing us to use properties
   * as a base and add per-execution
   *
   */

  @Parameter(required = false)
  private final Map<String, String> propertiesAppended = new HashMap<>();

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
  private final Map<String, File> fileToPropertiesArray = new HashMap<>();

  /**
   * Mappings added to the property above. This allows us to use
   * filesToPropertiesArray as a configured base and then add per-execution
   */
  @Parameter(required = false)
  private final Map<String, File> fileToPropertiesArrayAppended = new HashMap<>();

  /**
   * Files, in order, to read in and merge to make a single Properties object that
   * gets added to the existing properties
   *
   * Later files over-ride earlier files in this list, as the Properties created
   * uses each item as the "defaults" for each previous item.
   *
   */
  @Parameter(required = false)
  private final List<File> files = new ArrayList<>();
  /**
   * Files appended to the "files" parameter from above. This allows us to use
   * "files" as a base config and then add values to it per-execution
   */
  @Parameter(required = false)
  private final List<File> filesAppendeds = new ArrayList<>();

  /**
   * List of @{code sourceExtension} elements that are "source" files Defaults to
   * ["java", "scala", "groovy", "clj"]
   */
  @Parameter(required = false)
  private final Set<String> sourceExtensions = null;

  /**
   * Include files with names beginning with "."
   */
  @Parameter(required = true, defaultValue = "false")
  private boolean includeDotFiles;

  /**
   * Include files marked as "hidden" by the OS
   */
  @Parameter(required = true, defaultValue = "false")
  private boolean includeHidden;

  @Parameter(required = true, defaultValue = "false")
  private boolean caseSensitive;

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  private MavenProject project;

  @Parameter(required = true)
  private String engineHint;

  /**
   * This map is automatically populated from the dependency tree via plexus
   */
  @Component(role = TemplatingEngineSupplier.class)
  private Map<String, TemplatingEngineSupplier> suppliers;

  @Override
  public void execute() throws MojoExecutionException {
    if (!skip) {
      localExecute(getType(), mojo.getExecutionId(), isAppendExecutionIdentifierToOutput(), getSuppliers(), getEngine(),
          getProperties(), getPropertiesAppended(), getFileToPropertiesArray(), getFileToPropertiesArrayAppended(),
          getFiles(), getFilesAppendeds(), getScanningRootSource().toPath().getParent(), getScanningRootSource(),
          getOutputDirectory().toPath(), getSourceExtensions(), isIncludeDotFiles(), isIncludeHidden(), isDumpContext(),
          getProject(), getLog(), isCaseSensitive(), getPropertySuppliers(), getPropSuppliers());
    } else {
      getLog().info("Skipping templating for " + mojo.getExecutionId());
    }
  }

  public String getEngine() {
    return engineHint;
  }

  public List<File> getFiles() {
    return files;
  }

  public List<File> getFilesAppendeds() {
    return filesAppendeds;
  }

  public Map<String, File> getFileToPropertiesArray() {
    return fileToPropertiesArray;
  }

  public Map<String, File> getFileToPropertiesArrayAppended() {
    return fileToPropertiesArrayAppended;
  }

  public MavenProject getProject() {
    return requireNonNull(project);
  }

  public Properties getProperties() {
    return mapSS2Props.apply(properties);
  }

  public Properties getPropertiesAppended() {
    return mapSS2Props.apply(propertiesAppended);
  }

  public List<String> getPropertySuppliers() {
    return propertySuppliers;
  }

  public Map<String, MSOSupplier> getPropSuppliers() {
    return propSuppliers;
  }

  public Set<String> getSourceExtensions() {
    return sourceExtensions;
  }

  public Map<String, TemplatingEngineSupplier> getSuppliers() {
    return suppliers;
  }

  abstract public TemplateType getType();

  public boolean isAppendExecutionIdentifierToOutput() {
    return appendExecutionIdentifierToOutput;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isDumpContext() {
    return dumpContext;
  }

  public boolean isIncludeDotFiles() {
    return includeDotFiles;
  }

  public boolean isIncludeHidden() {
    return includeHidden;
  }

  protected abstract File getOutputDirectory();

  protected abstract File getScanningRootSource();

}
