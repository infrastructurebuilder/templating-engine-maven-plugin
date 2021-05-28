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
import static org.infrastructurebuilder.templating.maven.internal.IterativeTemplatingComponent.ITERATED_RESOURCES;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.infrastructurebuilder.templating.TemplatingEngineException;
import org.infrastructurebuilder.templating.maven.internal.InternalPlatform;

@Mojo(name = "attach", requiresProject = true, threadSafe = true, instantiationStrategy = PER_LOOKUP, defaultPhase = PACKAGE, requiresDependencyResolution = RUNTIME)
public final class IterativeResourcesTemplatingPackageMojo extends AbstractMojo {

  protected static final String[]   DEFAULT_EXCLUDES = new String[] { "**/package.html" };

  protected static final String[]   DEFAULT_INCLUDES = new String[] { "**/**" };

  @Parameter
  public String[]                   excludes         = DEFAULT_EXCLUDES;

  @Parameter(property = "templating.finalName", required = true, defaultValue = "${project.build.finalName}")
  public String                     finalName;

  @Parameter(property = "templating.force", required = false)
  public boolean                    force            = false;

  @Parameter
  public String[]                   includes         = DEFAULT_INCLUDES;

  @Component(role = Archiver.class, hint = "jar")
  public JarArchiver                archiver;

  @Component
  public MavenProjectHelper         mavenProjectHelper;

//  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
//  public MojoExecution              mojo;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  public File                       outputDirectory;

  @Parameter(property = "project", readonly = true, required = true)
  public MavenProject               project;

  @Parameter(required = false)
  public List<ClassifierNameMapper> idMappers        = new ArrayList<>();

  public File createArchive(Path contentDirectory, String classifier) throws MojoExecutionException {
    try {
      StringBuilder fileName = new StringBuilder(Objects.requireNonNull(finalName, "finalName")).append("-")
          .append(Objects.requireNonNull(classifier, "classifier")).append(".jar");
      final File    target   = new File(outputDirectory, fileName.toString());
      getLog().info("Archive file is " + target);
      archiver.setDestFile(target);
      archiver.setForced(force);
      getLog().info("Content dir = " + contentDirectory);
      DefaultFileSet fileSet = new DefaultFileSet(contentDirectory.toFile());
      fileSet.setIncludes(includes);
      fileSet.setExcludes(excludes);
      archiver.addFileSet(fileSet);
      archiver.createArchive();
      return target;
    } catch (final Exception e) {
      throw new MojoExecutionException("Error assembling archive", e);
    }
  }

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
      String classifier = idMappers.stream().filter(im -> im.matches(ip.getIdsJoinedDashString())).findFirst()
          .flatMap(q -> q.map(ip.getIdsJoinedDashString())).orElse(ip.getIdsJoinedDashString());
      getLog().info("Classifier set " + ip.getIdsJoinedDashString() + " -> " + classifier);

      et.withTranslation(() -> {
        try {
          final File a = createArchive(contentDir, classifier);
          getLog().info(String.format("Classifier set: %s", classifier));
          mavenProjectHelper.attachArtifact(project, "jar", classifier, a);
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
