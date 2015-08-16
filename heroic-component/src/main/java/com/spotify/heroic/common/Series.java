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

package com.spotify.heroic.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import lombok.ToString;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedMap;

@ToString(of = { "key", "tags" })
public class Series {
    static final SortedMap<String, String> EMPTY_TAGS = ImmutableSortedMap.<String, String> of();
    static final String EMPTY_STRING = "";

    final String key;
    final Map<String, String> tags;
    final long hash;

    /**
     * Package-private constructor to avoid invalid inputs.
     * 
     * @param key The key of the time series.
     * @param tags The tags of the time series.
     */
    @JsonCreator
    Series(@JsonProperty("key") String key, @JsonProperty("tags") SortedMap<String, String> tags) {
        this.key = key;
        this.tags = checkNotNull(tags, "tags");
        this.hash = generateHash();
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public long generateHash() {
        final long prime = 63;
        long result = 1;
        result = prime * result + ((key == null) ? 0 : stringHash(key));

        for (final Map.Entry<String, String> e : tags.entrySet()) {
            result = prime * result + stringHash(e.getKey());
            result = prime * result + stringHash(e.getValue());
        }

        return result;
    }

    private long stringHash(String string) {
        final long prime = 63;
        long result = 1;

        for (int i = 0; i < string.length(); i++) {
            result = prime * result + string.charAt(i);
        }

        return result;
    }

    public long hash() {
        return hash;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + tags.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (Series.class != obj.getClass()) {
            return false;
        }

        final Series o = (Series) obj;

        if (hash != o.hash) {
            return false;
        }

        if (key == null) {
            if (o.key != null) {
                return false;
            }

            return true;
        }

        if (o.key == null) {
            return false;
        }

        if (!key.equals(o.key)) {
            return false;
        }

        if (!tags.equals(o.tags)) {
            return false;
        }

        return true;
    }

    static final Series empty = new Series(null, EMPTY_TAGS);

    public static Series empty() {
        return empty;
    }

    public static Series of(String key) {
        return new Series(key, EMPTY_TAGS);
    }

    public static Series of(String key, Map<String, String> tags) {
        return of(key, tags.entrySet().iterator());
    }

    public static Series of(String key, Set<Map.Entry<String, String>> entries) {
        return of(key, entries.iterator());
    }

    public static Series of(String key, Iterator<Map.Entry<String, String>> tagPairs) {
        final ImmutableSortedMap.Builder<String, String> tags = ImmutableSortedMap.naturalOrder();

        while (tagPairs.hasNext()) {
            final Map.Entry<String, String> pair = tagPairs.next();
            final String tk = checkNotNull(pair.getKey());
            final String tv = checkNotNull(pair.getValue());
            tags.put(tk, tv);
        }

        return new Series(key, tags.build());
    }

    public Iterator<Map.Entry<String, String>> getTagsIterator() {
        return tags.entrySet().stream().map((e) -> (Map.Entry<String, String>) Pair.of(e.getKey(), e.getValue()))
                .iterator();
    }
}