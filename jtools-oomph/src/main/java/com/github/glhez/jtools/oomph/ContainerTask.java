package com.github.glhez.jtools.oomph;

public interface ContainerTask extends Task {
  CompoundTask addCompoundTask(String name);

  StringSubstitutionTask addStringSubstitutionTask(String name);
}
