/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.config;

import com.streamsets.pipeline.api.impl.Utils;

import java.util.List;

public class RawSourceDefinition {

  private final String rawSourcePreviewerClass;
  private final String mimeType;
  List<ConfigDefinition> configDefinitions;

  public RawSourceDefinition(String rawSourcePreviewerClass, String mimeType,
                             List<ConfigDefinition> configDefinitions) {
    this.rawSourcePreviewerClass = rawSourcePreviewerClass;
    this.configDefinitions = configDefinitions;
    this.mimeType = mimeType;
  }

  public String getRawSourcePreviewerClass() {
    return rawSourcePreviewerClass;
  }

  public List<ConfigDefinition> getConfigDefinitions() {
    return configDefinitions;
  }

  public String getMimeType() {
    return mimeType;
  }

  @Override
  public String toString() {
    return Utils.format("RawSourceDefinition[rawSourcePreviewerClass='{}' mimeType='{}']", getRawSourcePreviewerClass(),
      getMimeType());
  }
}