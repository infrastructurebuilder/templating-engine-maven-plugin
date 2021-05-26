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

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.infrastructurebuilder.util.core.PathSupplier;
import org.infrastructurebuilder.util.core.TSupplier;

@Named(TemplatingWorkingPathSupplier.TEMPLATING_WORKING_PATH)
@Singleton
public class TemplatingWorkingPathSupplier extends TSupplier<Path> implements PathSupplier {
  static final String TEMPLATING_WORKING_PATH = "templating-working-path";

  @Inject
  public TemplatingWorkingPathSupplier() {
    super();
  }

  public TemplatingWorkingPathSupplier(PathSupplier i) {
    this();
    this.setT(i.get());
  }
}
