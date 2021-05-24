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

import static org.apache.maven.plugins.annotations.InstantiationStrategy.PER_LOOKUP;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;
import static org.infrastructurebuilder.templating.TemplatingEngineException.et;
import static org.infrastructurebuilder.templating.maven.AbstractIterativeTemplatingMojo.ITERATED_RESOURCES;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.maven.internal.InternalPlatform;

@Mojo(name = "attach", requiresProject = true, threadSafe = true, instantiationStrategy = PER_LOOKUP, defaultPhase = PACKAGE, requiresDependencyResolution = RUNTIME)
public final class IterativeResourcesTemplatingPackageMojo extends AbstractIterativeTemplatingPackagingMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    @SuppressWarnings("rawtypes")
    Map                    pc       = getPluginContext();
    @SuppressWarnings("unchecked")
    List<InternalPlatform> attached = (List<InternalPlatform>) pc.getOrDefault(ITERATED_RESOURCES, new ArrayList<>());
    attached.forEach(ip -> {
      // Build an archive for each InternalPlatform
      getLog().info("Creating archive for " + ip);
      Path   contentDir = ip.getFinalDestination().orElseThrow();
      String classifier = ip.getPathIdString();
      et.withTranslation(() -> {
        try {
          final File a = createArchive(contentDir, classifier);
          getLog().info(String.format("Classifier set: %s", classifier));
          getMavenProjectHelper().attachArtifact(getProject(), "jar", classifier, a);
        } catch (final Throwable e) {
          getLog().error("Failed to create archive", e);
          throw new TemplatingEngineException("Failed to create archive!", e);
        }
      });
    });
    pc.remove(ITERATED_RESOURCES); // Remove the list of items once packaged
    setPluginContext(pc);
  }

}
