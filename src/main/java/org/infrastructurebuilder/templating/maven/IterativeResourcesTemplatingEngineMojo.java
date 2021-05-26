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
import static java.util.stream.Collectors.toList;
import org.infrastructurebuilder.templating.maven.internal.TemplatingUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.maven.internal.InternalPlatform;

@Mojo(name = "iterate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true)
public class IterativeResourcesTemplatingEngineMojo extends AbstractTemplatingMojo {
  public static final String ITERATED_RESOURCES = "ITERATED_RESOURCES";
  /**
   * ORDERED List of sub-platform types for platform-array-generation
   * Each sub platform will generated a pathed output from the root and all previous sub platforms
   *
   */
  @Parameter(required = true, defaultValue = "")
  private List<Platform>     platforms          = new ArrayList<>();

  @Parameter(required = false, defaultValue = "true")
  private boolean            addIdentifiers;

  /**
   * finalOverrides are applied LAST.  If anything needs to be over-ridden, this is the place to do it
   */
  @Parameter(required = false)
  private Properties         finalOverrides     = new Properties();

  @Parameter(required = false)
  private File               pathPropertiesRoot;

  @Parameter(required = false, defaultValue = "path.properties")
  private String             pathPropertiesName;
  /**
   * By default (and for convenience), we use the location of dependency:unpack-dependencies
   *
   * @required
   */
  @Parameter(required = true, defaultValue = "${project.build.directory}/dependency")
  private File               source;
  /**
   * Output directory for resources.
   *
   */
  @Parameter(required = false, defaultValue = "${project.build.directory}/generated-resources/iterated-templating-resources")
  private File               outputDirectory;

  @Override
  public void execute() throws MojoExecutionException {

    if (!skip) {
      @SuppressWarnings("rawtypes")
      Map                    pc           = getPluginContext();
      List<InternalPlatform> oldResources = (List<InternalPlatform>) pc.getOrDefault(ITERATED_RESOURCES,
          new ArrayList<>());

      if (platforms.size() < 1)
        throw new MojoExecutionException("At least one <platform> must be specified");
      List<InternalPlatform> ipl  = new ArrayList<>();
      Platform               root = platforms.get(0);
      List<InternalPlatform> l2   = root.getInstances().stream().map(pi -> new InternalPlatform(root).extend(pi))
          .collect(toList());
      ipl.addAll(l2);
      List<Platform> plist = new ArrayList<>();
      plist.addAll(platforms.subList(1, platforms.size()));
      while (plist.size() > 0) {
        List<InternalPlatform> newIpl = new ArrayList<>();
        Platform               recent = plist.get(0);

        List<InternalPlatform> wl     = ipl.stream().map(InternalPlatform::copy).collect(toList());
        recent.getInstances().forEach(pi -> {
          List<InternalPlatform> l = wl.stream().map(item -> item.extend(pi)).collect(toList());
          newIpl.addAll(l);
        });
        plist = plist.subList(1, plist.size());
        ipl = newIpl;
      }
      // ipl now holds the list of aggregated items (at the expense of a chunk of
      // memory)

      TemplateType t;
      switch (getType()) {
      case ITERATIVE_RESOURCE:
        t = TemplateType.RESOURCE;
        break;
      default:
        throw new TemplatingEngineException("Template type not accepted");
      }

      String pathPropertiesOrder = UUID.randomUUID().toString();
      String finalPropertiesId   = UUID.randomUUID().toString();
      for (int i = 0; i < ipl.size(); ++i) {
        InternalPlatform         iplItem = ipl.get(i);

        Path                     v       = iplItem.getExtendedPath(getOutputDirectory().toPath());
        Map<String, MSOSupplier> p       = new HashMap<>();

        p.putAll(propSuppliers);
        List<String> list = new ArrayList<>(propertySuppliers);
        list.add(iplItem.toString());
        p.put(iplItem.toString(), iplItem.getMSO(addIdentifiers));

        if (pathPropertiesRoot != null) {
          if (pathPropertiesRoot.isDirectory()) {
            Path pp = iplItem.getExtendedPath(pathPropertiesRoot.toPath()).resolve(this.pathPropertiesName)
                .toAbsolutePath();
            if (Files.exists(pp) && Files.isRegularFile(pp)) {
              Properties pp2 = new Properties();
              getLog().debug("Trying to read " + pp.toAbsolutePath());
              try (BufferedReader in = Files.newBufferedReader(pp)) {
                pp2.load(in);
              } catch (IOException e) {
                throw new TemplatingEngineException(e);
              }
              list.add(pathPropertiesOrder);
              p.put(pathPropertiesOrder, () -> TemplatingUtils.toMSO.apply(pp2));
              getLog().info("Added " + pp + " to properties");
            }

          }
        } else
          getLog().info("ignoring pathPropertiesRoot");

        list.add(finalPropertiesId);
        p.put(finalPropertiesId, () -> TemplatingUtils.toMSO.apply(finalOverrides));

        Path parentPath = getScanningRootSource().toPath();
        if (useSourceParent)
          parentPath = parentPath.getParent();
        TemplatingUtils.localExecute(t, mojo.getExecutionId(), /* isAppendExecutionIdentifierToOutput() */ false,
            suppliers, engineHint, TemplatingUtils.mapSS2Props.apply(properties),
            TemplatingUtils.mapSS2Props.apply(propertiesAppended), fileToPropertiesArray, fileToPropertiesArrayAppended,
            files, filesAppendeds, parentPath, getScanningRootSource(), v, sourceExtensions, includeDotFiles,
            includeHidden, dumpContext, includeSystemProperties, includeEnvironment,
            requireNonNull(project, "maven project"), getLog(), caseSensitive, list, p);
        iplItem.setFinalDestination(v);
        oldResources.add(iplItem);
      }
      pc.put(ITERATED_RESOURCES, oldResources);
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
  public File getScanningRootSource() {
    return source;
  }

  @Override
  public TemplateType getType() {
    return TemplateType.ITERATIVE_RESOURCE;
  }

}
