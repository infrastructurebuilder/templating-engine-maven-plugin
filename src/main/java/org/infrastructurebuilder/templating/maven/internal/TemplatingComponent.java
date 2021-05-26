package org.infrastructurebuilder.templating.maven.internal;

import javax.inject.Inject;
import javax.inject.Named;

@Named(TemplatingComponent.TEMPLATING_COMPONENT)
public class TemplatingComponent {

  public static final String TEMPLATING_COMPONENT = "templating-component";

  @Inject
  public TemplatingComponent() {
  }

  public void execute() {

  }

}
