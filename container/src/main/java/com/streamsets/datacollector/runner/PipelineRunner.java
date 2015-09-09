/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.runner;

import com.codahale.metrics.MetricRegistry;
import com.streamsets.datacollector.main.RuntimeInfo;
import com.streamsets.datacollector.runner.production.BadRecordsHandler;
import com.streamsets.pipeline.api.StageException;

import java.util.List;

public interface PipelineRunner {

  public RuntimeInfo getRuntimeInfo();

  public boolean isPreview();

  public MetricRegistry getMetrics();

  public void run(Pipe[] pipes, BadRecordsHandler badRecordsHandler) throws StageException, PipelineRuntimeException;

  public void run(Pipe[] pipes, BadRecordsHandler badRecordsHandler, List<StageOutput> stageOutputsToOverride)
      throws StageException, PipelineRuntimeException;

  public List<List<StageOutput>> getBatchesOutput();

  public String getSourceOffset();

  public String getNewSourceOffset();

  public void setObserver(Observer observer);

  public void registerListener(BatchListener batchListener);

}