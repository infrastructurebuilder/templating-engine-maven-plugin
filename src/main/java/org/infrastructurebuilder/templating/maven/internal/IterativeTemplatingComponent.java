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

import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.TemplatingEngineException;
//import org.infrastructurebuilder.templating.maven.Platform;
import org.infrastructurebuilder.templating.maven.TemplateType;

@Named(IterativeTemplatingComponent.ITERATIVE_TEMPLATING_COMPONENT)
public class IterativeTemplatingComponent extends TemplatingComponent {
  public static final String    ITERATED_RESOURCES             = "ITERATED_RESOURCES";

  public static final String    ITERATIVE_TEMPLATING_COMPONENT = "iterative-templating-component";

  public List<InternalPlatform> internalPlatformList           = new ArrayList<>();

  public Properties             finalOverrides;

  public boolean                addIdentifiers;

  public Path                   pathPropertiesRoot;

  public String                 pathPropertiesName;

  public final void execute(@SuppressWarnings("rawtypes") Map pc) throws MojoExecutionException {
    @SuppressWarnings("unchecked")
    List<InternalPlatform> oldResources = ((List<InternalPlatform>) pc.getOrDefault(ITERATED_RESOURCES,
        new ArrayList<>()));

    // internalPlatformList now holds the list of aggregated items

    TemplateType           t;
    switch (type) {
    case ITERATIVE_RESOURCE:
      t = TemplateType.RESOURCE;
      break;
    default:
      throw new TemplatingEngineException("Template type not accepted");
    }

    String pathPropertiesOrder = UUID.randomUUID().toString();
    String finalPropertiesId   = UUID.randomUUID().toString();
    for (int i = 0; i < internalPlatformList.size(); ++i) {
      InternalPlatform         iplItem                    = internalPlatformList.get(i);
      Path                     workingExtendedPath        = iplItem.getExtendedPath(outputDirectory);
      Map<String, MSOSupplier> workingPropertySupplierMap = new HashMap<>();

      workingPropertySupplierMap.putAll(propertySupplierMap);
      List<String> workingPropertySupplierKeys = new ArrayList<>(propertySupplierKeys);
      String       iplKey                      = iplItem.toString();
      workingPropertySupplierKeys.add(iplKey);
      workingPropertySupplierMap.put(iplKey, iplItem.getMSO(addIdentifiers));

      if (pathPropertiesRoot != null) {
        if (Files.isDirectory(pathPropertiesRoot)) {
          Path pp = iplItem.getExtendedPath(pathPropertiesRoot).resolve(this.pathPropertiesName).toAbsolutePath();
          if (Files.exists(pp) && Files.isRegularFile(pp)) {
            Properties pp2 = new Properties();
            getLog().debug("Trying to read " + pp.toAbsolutePath());
            try (BufferedReader in = Files.newBufferedReader(pp)) {
              pp2.load(in);
            } catch (IOException e) {
              throw new TemplatingEngineException(e);
            }
            workingPropertySupplierKeys.add(pathPropertiesOrder);
            workingPropertySupplierMap.put(pathPropertiesOrder, () -> TemplatingUtils.toMSO.apply(pp2));
            getLog().info("Added " + pp + " to properties");
          }

        }
      } else
        getLog().info("ignoring pathPropertiesRoot");

      workingPropertySupplierKeys.add(finalPropertiesId);
      workingPropertySupplierMap.put(finalPropertiesId, () -> TemplatingUtils.toMSO.apply(finalOverrides));

      Path parentPath = useSourceParent ? scanningRootSource : scanningRootSource.getParent();
      TemplatingUtils.localExecute(t, executionId, /* isAppendExecutionIdentifierToOutput() */ false, comp, properties,
          propertiesAppended, fileToPropertiesArray, fileToPropertiesArrayAppended, files, parentPath,
          scanningRootSource, workingExtendedPath, systemProperties, env, requireNonNull(project, "maven project"),
          getLog(), workingPropertySupplierKeys, workingPropertySupplierMap);
      iplItem.setFinalDestination(workingExtendedPath);
      oldResources.add(iplItem);
    }
    pc.put(ITERATED_RESOURCES, oldResources);

  }

}
