/**
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zuiyouliao.cache.core;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Map;

/**
 * 自定义带ttl的redis缓存管理器
 *
 * @author lzc
 * @date 2021/03/08 16:25
 */
public class TtlRedisCacheManager extends RedisCacheManager {

    private final RedisCacheConfiguration defaultCacheConfiguration;

    public TtlRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCacheConfigurations) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations);
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }

    @Override
    public RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        return super.createRedisCache(name, cacheConfig);
    }

    public RedisCacheConfiguration getCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultCacheConfiguration.getTtl())
                .serializeKeysWith(defaultCacheConfiguration.getKeySerializationPair())
                .serializeValuesWith(defaultCacheConfiguration.getValueSerializationPair())
                .withConversionService(defaultCacheConfiguration.getConversionService())
                .computePrefixWith(defaultCacheConfiguration::getKeyPrefixFor);
    }
}
