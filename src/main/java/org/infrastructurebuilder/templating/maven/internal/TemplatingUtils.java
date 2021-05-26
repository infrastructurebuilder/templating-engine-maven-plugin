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

import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.templating.TemplatingEngine.EXECUTION_IDENTIFIER;
import static org.infrastructurebuilder.templating.TemplatingEngine.mergeProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.infrastructurebuilder.templating.AbstractMavenBackedPropertiesSupplier;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.TemplatingEngine;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.TemplatingEngineSupplier;
import org.infrastructurebuilder.templating.maven.TemplateType;
import org.infrastructurebuilder.util.core.IBUtils;
import org.json.JSONArray;

public final class TemplatingUtils {

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

  public final static Function<Map<String, Path>, Map<String, JSONArray>> _getSingleMap = (m) -> {
    final Map<String, JSONArray> map = new HashMap<>();
    requireNonNull(m).entrySet().forEach(e -> {
      if (Files.isRegularFile(e.getValue()) && Files.isReadable(e.getValue())) {
        try {
          final JSONArray arry = new JSONArray();
          Files.readAllLines(e.getValue())
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
          throw new TemplatingEngineException("Failed to read " + e.getValue(), e1);
        }
      }
    });
    return map;
  };

  public static Map<String, Object> generateFileToPropertiesArray(final Map<String, Path> array,
      final Map<String, Path> appended) throws MojoExecutionException {
    return extendWithAll(_getSingleMap.apply(array), _getSingleMap.apply(appended));
  }

  public final static Function<Properties, Map<String, Object>> toMSO = (p) -> {
    return requireNonNull(p).stringPropertyNames().stream()
        .collect(Collectors.toMap(Function.identity(), v -> p.getProperty(v)));
  };

  public static Map<String, Object> getFilesProperties(final List<Path> workingFiles) throws TemplatingEngineException {
    Map<String, Object> p = new HashMap<>();
    for (final Path f : workingFiles) {
      if (isRegularFile(f) && isReadable(f)) {
        try (InputStream ins = Files.newInputStream(f)) {
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
   * @param msoSupplierMap
   * @throws MojoExecutionException
   */
  public final static void localExecute(final TemplateType type, final String executionIdentifier,
      final boolean appendExecutionIdentifierToOutput, final TemplatingEngineSupplier comp,
      final Map<String, Object> properties, final Map<String, Object> propertiesAppended,
      final Map<String, Path> fileToPropertiesArray, final Map<String, Path> fileToPropertiesArrayAppended,
      final List<Path> files, final Path sourcePathRoot, final Path templateSources, Path sourcesOutputDirectory,
      Map<String, Object> systemProperties, final Map<String, Object> env, final MavenProject optProject,
      final Logger log, final List<String> msoSuppliersKeys, final Map<String, MSOSupplier> msoSupplierMap)
      throws MojoExecutionException {

    Map<String, Object> real;
    try {
      real = mergeProperties(
          // Overriding Order
          systemProperties,
          //
          env,
          // Get all properties from files and filesAppendeds
          getFilesProperties(files),
          // Merged with all array properties and appendeds
          generateFileToPropertiesArray(fileToPropertiesArray, fileToPropertiesArrayAppended),
          // And finall all the properties
          properties,
          // And then all appended properties
          propertiesAppended);

      // THEN we use the suppliers
      for (final String supplierKey : msoSuppliersKeys) { // Some suppliers need addl configuration
        final MSOSupplier pv = requireNonNull(msoSupplierMap.get(supplierKey), "MSOSupplier " + supplierKey + " not found");
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
    comp.setProperties(real);
    comp.setSourcesOutputDirectory(sourcesOutputDirectory);

    try {
      comp.setSourcePathRoot(sourcePathRoot);
      comp.setExecutionSource(templateSources);
      final Optional<String> s = comp.get().execute();
      if (s.isPresent()) {
        log.debug("Context for main execution is [probably]: \n" + s.get());

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
        case ITERATIVE_RESOURCE:
          throw new MojoExecutionException("Cannot process interative resources here");
        }
      }
    } catch (final Exception e) {
      throw new MojoExecutionException("Failed to execute TemplatingEngine", e);
    }

  }

}
