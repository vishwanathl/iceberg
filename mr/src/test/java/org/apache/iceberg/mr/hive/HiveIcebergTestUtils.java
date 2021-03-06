/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iceberg.mr.hive;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.hadoop.hive.serde2.io.TimestampWritable;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StandardStructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.mr.hive.serde.objectinspector.IcebergBinaryObjectInspector;
import org.apache.iceberg.mr.hive.serde.objectinspector.IcebergDecimalObjectInspector;
import org.apache.iceberg.mr.hive.serde.objectinspector.IcebergObjectInspector;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.ByteBuffers;
import org.apache.iceberg.util.UUIDUtil;
import org.junit.Assert;

import static org.apache.iceberg.types.Types.NestedField.optional;

public class HiveIcebergTestUtils {
  // TODO: Can this be a constant all around the Iceberg tests?
  public static final Schema FULL_SCHEMA = new Schema(
      optional(1, "boolean_type", Types.BooleanType.get()),
      optional(2, "integer_type", Types.IntegerType.get()),
      optional(3, "long_type", Types.LongType.get()),
      optional(4, "float_type", Types.FloatType.get()),
      optional(5, "double_type", Types.DoubleType.get()),
      optional(6, "date_type", Types.DateType.get()),
      // TimeType is not supported
      // required(7, "time_type", Types.TimeType.get()),
      optional(7, "tsTz", Types.TimestampType.withZone()),
      optional(8, "ts", Types.TimestampType.withoutZone()),
      optional(9, "string_type", Types.StringType.get()),
      optional(10, "uuid_type", Types.UUIDType.get()),
      optional(11, "fixed_type", Types.FixedType.ofLength(3)),
      optional(12, "binary_type", Types.BinaryType.get()),
      optional(13, "decimal_type", Types.DecimalType.of(38, 10)));

  public static final StandardStructObjectInspector FULL_SCHEMA_OBJECT_INSPECTOR =
      ObjectInspectorFactory.getStandardStructObjectInspector(
          // Capitalized `boolean_type` field to check for field case insensitivity.
          Arrays.asList("Boolean_Type", "integer_type", "long_type", "float_type", "double_type",
              "date_type", "tsTz", "ts", "string_type", "uuid_type", "fixed_type", "binary_type", "decimal_type"),
          Arrays.asList(
              PrimitiveObjectInspectorFactory.writableBooleanObjectInspector,
              PrimitiveObjectInspectorFactory.writableIntObjectInspector,
              PrimitiveObjectInspectorFactory.writableLongObjectInspector,
              PrimitiveObjectInspectorFactory.writableFloatObjectInspector,
              PrimitiveObjectInspectorFactory.writableDoubleObjectInspector,
              IcebergObjectInspector.DATE_INSPECTOR,
              IcebergObjectInspector.TIMESTAMP_INSPECTOR_WITH_TZ,
              IcebergObjectInspector.TIMESTAMP_INSPECTOR,
              PrimitiveObjectInspectorFactory.writableStringObjectInspector,
              PrimitiveObjectInspectorFactory.writableStringObjectInspector,
              IcebergBinaryObjectInspector.byteArray(),
              IcebergBinaryObjectInspector.byteBuffer(),
              IcebergDecimalObjectInspector.get(38, 10)
          ));

  private HiveIcebergTestUtils() {
    // Empty constructor for the utility class
  }

  public static Record getTestRecord(boolean uuidAsByte) {
    Record record = GenericRecord.create(HiveIcebergTestUtils.FULL_SCHEMA);
    record.set(0, true);
    record.set(1, 1);
    record.set(2, 2L);
    record.set(3, 3.1f);
    record.set(4, 4.2d);
    record.set(5, LocalDate.of(2020, 1, 21));
    // TimeType is not supported
    // record.set(6, LocalTime.of(11, 33));
    // Nano is not supported ?
    record.set(6, OffsetDateTime.of(2017, 11, 22, 11, 30, 7, 0, ZoneOffset.ofHours(2)));
    record.set(7, LocalDateTime.of(2019, 2, 22, 9, 44, 54));
    record.set(8, "kilenc");
    if (uuidAsByte) {
      // TODO: Parquet UUID expect byte[], others are expecting UUID
      record.set(9, UUIDUtil.convert(UUID.fromString("1-2-3-4-5")));
    } else {
      record.set(9, UUID.fromString("1-2-3-4-5"));
    }
    record.set(10, new byte[]{0, 1, 2});
    record.set(11, ByteBuffer.wrap(new byte[]{0, 1, 2, 3}));
    record.set(12, new BigDecimal("0.0000000013"));

    return record;
  }

  public static Record getNullTestRecord() {
    Record record = GenericRecord.create(HiveIcebergTestUtils.FULL_SCHEMA);

    for (int i = 0; i < HiveIcebergTestUtils.FULL_SCHEMA.columns().size(); i++) {
      record.set(i, null);
    }

    return record;
  }

  public static List<Object> valuesForTestRecord(Record record) {
//    ByteBuffer byteBuffer = record.get(11, ByteBuffer.class);
//    byte[] bytes = new byte[byteBuffer.remaining()];
//    byteBuffer.mark();
//    byteBuffer.get(bytes);
//    byteBuffer.reset();

    return Arrays.asList(
        new BooleanWritable(Boolean.TRUE),
        new IntWritable(record.get(1, Integer.class)),
        new LongWritable(record.get(2, Long.class)),
        new FloatWritable(record.get(3, Float.class)),
        new DoubleWritable(record.get(4, Double.class)),
        new DateWritable((int) record.get(5, LocalDate.class).toEpochDay()),
        // TimeType is not supported
        // new Timestamp()
        new TimestampWritable(Timestamp.from(record.get(6, OffsetDateTime.class).toInstant())),
        new TimestampWritable(Timestamp.valueOf(record.get(7, LocalDateTime.class))),
        new Text(record.get(8, String.class)),
        new Text(record.get(9, UUID.class).toString()),
        new BytesWritable(record.get(10, byte[].class)),
        new BytesWritable(ByteBuffers.toByteArray(record.get(11, ByteBuffer.class))),
        new HiveDecimalWritable(HiveDecimal.create(record.get(12, BigDecimal.class)))
    );
  }

  public static void assertEquals(Record expected, Record actual) {
    for (int i = 0; i < expected.size(); ++i) {
      if (expected.get(i) instanceof OffsetDateTime) {
        // For OffsetDateTime we just compare the actual instant
        Assert.assertEquals(((OffsetDateTime) expected.get(i)).toInstant(),
            ((OffsetDateTime) actual.get(i)).toInstant());
      } else {
        if (expected.get(i) instanceof byte[]) {
          Assert.assertArrayEquals((byte[]) expected.get(i), (byte[]) actual.get(i));
        } else {
          Assert.assertEquals(expected.get(i), actual.get(i));
        }
      }
    }
  }
}
