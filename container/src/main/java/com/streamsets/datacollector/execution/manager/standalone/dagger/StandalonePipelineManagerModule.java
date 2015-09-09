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
package com.streamsets.datacollector.execution.manager.standalone.dagger;

import com.streamsets.datacollector.execution.executor.ExecutorModule;
import com.streamsets.datacollector.execution.manager.standalone.StandaloneAndClusterPipelineManager;
import com.streamsets.datacollector.execution.preview.common.dagger.PreviewerProviderModule;
import com.streamsets.datacollector.execution.runner.provider.dagger.StandaloneAndClusterRunnerProviderModule;
import com.streamsets.datacollector.execution.snapshot.cache.dagger.CacheSnapshotStoreModule;
import com.streamsets.datacollector.execution.store.CachePipelineStateStoreModule;
import com.streamsets.datacollector.store.CachePipelineStoreModule;

import dagger.Module;

/**
 * Provides a singleton instance of Manager.
 */
@Module(library = true, injects = {StandaloneAndClusterPipelineManager.class},
  includes = {CachePipelineStateStoreModule.class, CachePipelineStoreModule.class, ExecutorModule.class,
    PreviewerProviderModule.class, StandaloneAndClusterRunnerProviderModule.class, CacheSnapshotStoreModule.class})
public class StandalonePipelineManagerModule {

}