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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.infrastructurebuilder.templating.TemplatingEngineException;

public class PlatformInstance {
  public static final String  NON_NULL = " cannot be null";

  private static final String ERR_STR  = "Only one of <properties>, <relPath>, or <path> is allowed";

  private String              id;
  private String              dirName;
  private String              path;
  private Properties          properties;
  private Platform            platform;

  public String getDirName() {
    return dirName == null ? getId() : requireNonNull(dirName, "dirName" + NON_NULL);
  }

  public void setPath(String path) {
    if (properties != null )
      throw new TemplatingEngineException(ERR_STR);
    this.path = path;
  }

  public void setProperties(Properties properties) {
    if (path != null )
      throw new TemplatingEngineException(ERR_STR);
    this.properties = properties;
  }

  public String getId() {
    return requireNonNull(id, "id" + NON_NULL);
  }

  public Optional<Path> getPath() {
    return Optional.ofNullable(path).map(Path::of);
  }

  public Properties getProperties() {
    if (properties == null) {

      // We must have a path or it's an error;
      properties = new Properties();
      Path r = Path.of(path);
      try (BufferedReader i = Files.newBufferedReader(r)) {
        properties.load(i);
      } catch (IOException e) {
        throw new TemplatingEngineException(e);
      }
    }
    return properties;

  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public Platform getPlatform() {
    return platform;
  }
}
