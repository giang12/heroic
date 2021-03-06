/*
 * Copyright (c) 2015 Spotify AB.
 *
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

package com.spotify.heroic.metric.datastax.schema.ng;

import com.spotify.heroic.metric.datastax.MetricsRowKey;
import com.spotify.heroic.metric.datastax.MetricsRowKey_Serializer;
import com.spotify.heroic.metric.datastax.TypeSerializer;
import eu.toolchain.serializer.BytesSerialWriter;
import eu.toolchain.serializer.SerialReader;
import eu.toolchain.serializer.Serializer;
import eu.toolchain.serializer.SerializerFramework;
import eu.toolchain.serializer.TinySerializer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MetricsRowKeySerializer implements TypeSerializer<MetricsRowKey> {
    final SerializerFramework s = TinySerializer.builder().useCompactSize(true).build();
    final Serializer<MetricsRowKey> serializer = new MetricsRowKey_Serializer(s, s.variableLong());

    @Override
    public ByteBuffer serialize(MetricsRowKey value) throws IOException {
        try (final BytesSerialWriter w = s.writeBytes()) {
            serializer.serialize(w, value);
            return w.toByteBuffer();
        }
    }

    @Override
    public MetricsRowKey deserialize(ByteBuffer buffer) throws IOException {
        try (final SerialReader r = s.readByteBuffer(buffer)) {
            return serializer.deserialize(r);
        }
    }
}
