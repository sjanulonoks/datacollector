/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
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
package com.streamsets.pipeline.stage.it;

import com.google.common.collect.ImmutableSortedMap;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.stage.HiveMetadataProcessorBuilder;
import com.streamsets.pipeline.stage.HiveMetastoreTargetBuilder;
import com.streamsets.pipeline.stage.destination.hive.HiveMetastoreTarget;
import com.streamsets.pipeline.stage.processor.hive.HiveMetadataProcessor;
import com.streamsets.pipeline.stage.processor.hive.PartitionConfig;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Test scenario where input origin is not able to give us full schema of the source data and is rather sending
 * only a subset of the structure with each record (for example typically CDC).
 */
public class PartialInputIT extends  BaseHiveMetadataPropagationIT {

  @Test
  public void testPartialInput() throws Exception {
    HiveMetadataProcessor processor = new HiveMetadataProcessorBuilder()
      .partitions(Collections.<PartitionConfig>emptyList())
      .build();
    HiveMetastoreTarget hiveTarget = new HiveMetastoreTargetBuilder()
      .build();

    List<Record> records = new LinkedList<>();
    Record record;

    record = RecordCreator.create("s", "s:1");
    record.set(Field.create(Field.Type.MAP, ImmutableSortedMap.of(
      "id", Field.create(Field.Type.STRING, "text")
    )));
    records.add(record);

    record = RecordCreator.create("s", "s:2");
    record.set(Field.create(Field.Type.MAP, ImmutableSortedMap.of(
      "name", Field.create(Field.Type.STRING, "text")
    )));
    records.add(record);

    record = RecordCreator.create("s", "s:3");
    record.set(Field.create(Field.Type.MAP, ImmutableSortedMap.of(
      "value", Field.create(Field.Type.STRING, "text")
    )));
    records.add(record);

    record = RecordCreator.create("s", "s:4");
    record.set(Field.create(Field.Type.MAP, ImmutableSortedMap.of(
      "value", Field.create(Field.Type.STRING, "text"),
      "id", Field.create(Field.Type.STRING, "text")
    )));
    records.add(record);

    record = RecordCreator.create("s", "s:5");
    record.set(Field.create(Field.Type.MAP, ImmutableSortedMap.of(
      "name", Field.create(Field.Type.STRING, "text"),
      "id", Field.create(Field.Type.STRING, "text")
    )));
    records.add(record);

    processRecords(processor, hiveTarget, records);

    // End state should be with three columns
    assertTableStructure("default.tbl",
      new ImmutablePair("tbl.id", Types.VARCHAR),
      new ImmutablePair("tbl.name", Types.VARCHAR),
      new ImmutablePair("tbl.value", Types.VARCHAR)
    );

    // 5 rows
    assertQueryResult("select count(*) from tbl", new QueryValidator() {
      @Override
      public void validateResultSet(ResultSet rs) throws Exception {
        Assert.assertTrue(rs.next());
        Assert.assertEquals(5, rs.getInt(1));
        Assert.assertFalse(rs.next());
      }
    });

    // And 4 files
    Assert.assertEquals(3, getDefaultFileSystem().listStatus(new Path("/user/hive/warehouse/tbl/")).length);
  }

}
