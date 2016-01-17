/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.testing;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.building.ModelCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A model builder cache backed by the repository system cache.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelCache implements ModelCache {

  private final TeslaModelCache cache;
  
  public DefaultModelCache() {
    //this.cache = new EntrySizeCache();
    this.cache = new SimpleCache();
  }

  public Object get(String groupId, String artifactId, String version, String tag) {
    return cache.get(new Key(groupId, artifactId, version, tag));
  }

  public void put(String groupId, String artifactId, String version, String tag, Object data) {
    cache.put(new Key(groupId, artifactId, version, tag), data);
  }

  static class Key {

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String tag;

    private final int hash;

    public Key(String groupId, String artifactId, String version, String tag) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.tag = tag;

      int h = 17;
      h = h * 31 + this.groupId.hashCode();
      h = h * 31 + this.artifactId.hashCode();
      h = h * 31 + this.version.hashCode();
      h = h * 31 + this.tag.hashCode();
      hash = h;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (null == obj || !getClass().equals(obj.getClass())) {
        return false;
      }

      Key that = (Key) obj;
      return artifactId.equals(that.artifactId) && groupId.equals(that.groupId) && version.equals(that.version) && tag.equals(that.tag);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }
  
  public interface TeslaModelCache {
    public void put(Key key, Object model);
    public Object get(Key key);
  }
  
  public class SimpleCache implements TeslaModelCache {

    private Map<Key, Object> cache = new ConcurrentHashMap<Key, Object>( 256 );

    public void put(Key key, Object model) {
      cache.put(key, model);
    }

    public Object get(Key key) {
      return cache.get(key);
    }
    
  }
  
  public class EntrySizeCache implements TeslaModelCache {

    private static final long MAX_SIZE = 10000;

    private final Cache<Key,Object> cache;

    public EntrySizeCache() {
      cache = CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build();
    }
    
    public void put(Key key, Object data) {
      cache.put(key, data);
    }
    
    public Object get(Key key) {      
      return cache.getIfPresent(key);
    }
  }  
}
