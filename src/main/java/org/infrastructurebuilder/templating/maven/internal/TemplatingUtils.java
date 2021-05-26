package org.infrastructurebuilder.templating.maven.internal;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
    Map<String, Object> p            = new HashMap<>();
    final List<File>    workingFiles = new ArrayList<>();
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
      final boolean includeHidden, final boolean includeSystemProperties, final boolean includeSystemEnv,
      final boolean dumpContext, final MavenProject optProject, final Log log, final boolean caseSensitive,
      final List<String> list, final Map<String, MSOSupplier> map) throws MojoExecutionException {

    TemplatingEngineSupplier comp;

    comp = Optional.ofNullable(suppliers.get(engineHint))
        .orElseThrow(() -> new MojoExecutionException("No engineHint supplier named '" + engineHint + "'"));

    Map<String, Object> real;
    Map<String, Object> env = new HashMap<>();
    if (includeSystemEnv)
      env.putAll(System.getenv());
    try {
      real = mergeProperties(toMSO.apply(includeSystemProperties ? System.getProperties() : new Properties()), env,
          // Get all properties from files and filesAppendeds
          getFilesProperties(files, filesAppendeds),
          // Merged with all array properties and appendeds
          extendWithAll(
              // File to proerptiesArray
              _getSingleMap.apply(fileToPropertiesArray),
              // File to properties array appended
              _getSingleMap.apply(fileToPropertiesArrayAppended)
          //
          ),

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
        case ITERATIVE_RESOURCE:
          throw new MojoExecutionException("Cannot process interative resources here");
        }
      }
    } catch (final Exception e) {
      throw new MojoExecutionException("Failed to execute TemplatingEngine", e);
    }

  }

}
