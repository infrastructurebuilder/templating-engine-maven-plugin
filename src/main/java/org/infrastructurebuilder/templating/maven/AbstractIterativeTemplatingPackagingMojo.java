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

import static java.nio.file.Files.createDirectories;
import static org.infrastructurebuilder.templating.TemplatingEngineException.et;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

abstract public class AbstractIterativeTemplatingPackagingMojo extends AbstractMojo {

  protected static final String[]    DEFAULT_EXCLUDES = new String[] { "**/package.html" };

  protected static final String[]    DEFAULT_INCLUDES = new String[] { "**/**" };

  @Component
  private ArchiverManager            archiverManager;

  @Component
  private ArtifactHandlerManager     artifactHandlerManager;

  @Parameter(property = "classifier")
  private String                     classifier;

  @Parameter
  private String[]                   excludes         = new String[0];

  @Parameter(property = "templating.finalName", required = true, defaultValue = "${project.build.finalName}")
  private String                     finalName;

  @Parameter(property = "templating.force", required = false)
  private boolean                    force            = false;

  @Parameter
  private String[]                   includes         = new String[0];

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver                jarArchiver;

  @Component
  protected MavenProjectHelper         mavenProjectHelper;

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  private MojoExecution              mojo;

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private File                       outputDirectory;

  @Parameter(property = "project", readonly = true, required = true)
  protected MavenProject               project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession               session;

  @Parameter(required = true, defaultValue = "${basedir}/src/main")
  private File                       sources;

  @Parameter(required = false)
  private List<ClassifierNameMapper> idMappers        = new ArrayList<>();

  @Parameter(defaultValue = "${project.build.directory}/templating-tmp", readonly = true, required = true)
  private File                       workDirectory;

  public Path getOutputDirectory() {
    return outputDirectory.toPath().toAbsolutePath();
  }

  public Path getSources() {
    return sources.toPath();
  }

  public Path getWorkDirectory() {
    return workDirectory.toPath().toAbsolutePath();
  }

  public void setWorkDirectory(final File workDirectory) {
    this.workDirectory = workDirectory;
    et.withTranslation(() -> createDirectories(getWorkDirectory()));
  }

  protected File getJarFile(final File basedir, final String resultFinalName, final String classifier) {
    if (basedir == null)
      throw new IllegalArgumentException("basedir is not allowed to be null");
    if (resultFinalName == null)
      throw new IllegalArgumentException("finalName is not allowed to be null");

    final StringBuilder fileName = new StringBuilder(resultFinalName);

    if (classifier != null) {
      fileName.append("-").append(classifier);
    }

    fileName.append(".jar");

    return new File(basedir, fileName.toString());
  }

  public File createArchive(Path contentDirectory, String classifier) throws MojoExecutionException {

    try {
      Archiver   archiver = archiverManager.getArchiver("jar");
      final File target   = getJarFile(outputDirectory, finalName, classifier);
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

  public List<ClassifierNameMapper> getIdMappers() {
    return idMappers;
  }
}
