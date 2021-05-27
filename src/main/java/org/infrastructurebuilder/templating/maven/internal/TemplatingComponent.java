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
import static org.infrastructurebuilder.templating.maven.internal.TemplatingUtils.mapSS2Props;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.infrastructurebuilder.templating.MSOSupplier;
import org.infrastructurebuilder.templating.TemplatingEngineSupplier;
import org.infrastructurebuilder.templating.maven.TemplateType;

@Named(TemplatingComponent.TEMPLATING_COMPONENT)
public class TemplatingComponent implements LogEnabled {

  public static final String      TEMPLATING_COMPONENT = "templating-component";
  private Logger                  logger;
  public TemplateType             type;
  public Path                     outputDirectory;
  public MavenProject             project;
  public boolean                  appendExecutionIdentifierToOutput;
  public boolean                  useSourceParent;
  public String                   executionId;
  public Map<String, Object>      env;
  public List<Path>               files;
  public Map<String, Object>      propertiesAppended;
  public Map<String, Object>      properties;
  public Map<String, Path>        fileToPropertiesArray;
  public Map<String, Path>        fileToPropertiesArrayAppended;
  public Set<String>              sourceExtensions;
  public List<String>             propertySupplierKeys;
  public Map<String, MSOSupplier> propertySupplierMap;
  public Path                     scanningRootSource;
  public TemplatingEngineSupplier templatingEngineSupplier;
  public Map<String, Object>      systemProperties;

  @Inject
  public TemplatingComponent() {
  }

  public void execute(@SuppressWarnings("rawtypes") Map pluginContext) throws MojoExecutionException {
    Path parentPath = useSourceParent ? scanningRootSource : scanningRootSource.getParent();

    TemplatingUtils.localExecute(type, executionId, appendExecutionIdentifierToOutput, templatingEngineSupplier, properties,
        propertiesAppended, fileToPropertiesArray, fileToPropertiesArrayAppended, files, parentPath, scanningRootSource,
        outputDirectory, systemProperties, env, requireNonNull(project, "maven project"), getLog(),
        propertySupplierKeys, propertySupplierMap);

  }

  @Override
  public void enableLogging(Logger logger) {
    this.logger = requireNonNull(logger);
  }

  public Logger getLog() {
    return logger;
  }

}
