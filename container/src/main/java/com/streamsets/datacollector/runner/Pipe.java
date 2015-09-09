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


import com.streamsets.datacollector.validation.Issue;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.impl.Utils;

import java.util.List;

public abstract class Pipe<C extends Pipe.Context> {
  private final StageRuntime stage;
  private final List<String> inputLanes;
  private final List<String> outputLanes;

  public Pipe(StageRuntime stage, List<String> inputLanes, List<String> outputLanes) {
    this.stage = stage;
    this.inputLanes = inputLanes;
    this.outputLanes = outputLanes;
  }

  public StageRuntime getStage() {
    return stage;
  }

  public List<String> getInputLanes() {
    return inputLanes;
  }

  public List<String> getOutputLanes() {
    return outputLanes;
  }

  public abstract List<Issue> init(C pipeContext) throws StageException;

  public abstract void process(PipeBatch pipeBatch) throws StageException, PipelineRuntimeException;

  public abstract void destroy();

  public interface Context {

  }

  @Override
  public String toString() {
    return Utils.format("{}[instance='{}' input='{}' output='{}']", getClass().getSimpleName(),
                        getStage().getInfo().getInstanceName(), getInputLanes(), getOutputLanes());
  }
}