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
package com.zuiyouliao.cache.configuration;

import com.zuiyouliao.cache.constant.CacheConstant;
import com.zuiyouliao.cache.core.TtlCacheKeyGenerator;
import com.zuiyouliao.cache.core.TtlRedisCacheManager;
import com.zuiyouliao.cache.core.TtlRedisCacheResolver;
import com.zuiyouliao.cache.prop.ProjectProperties;
import com.zuiyouliao.cache.prop.SerialProperties;
import com.zuiyouliao.cache.prop.TaskProperties;
import com.zuiyouliao.cache.task.CacheAccessRegistrar;
import com.zuiyouliao.cache.task.CacheRefresher;
import com.zuiyouliao.cache.task.CacheTask;
import com.zuiyouliao.cache.util.SpringContextUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存自动配置
 *
 * @author lzc
 * @date 2021/03/08 9:42
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({TaskProperties.class, SerialProperties.class, ProjectProperties.class})
public class CacheAutoConfiguration {

    @Resource
    RedisConnectionFactory redisConnectionFactory;
    @Resource
    private TaskProperties taskProperties;
    @Resource
    private SerialProperties serialProperties;

    @Bean
    @ConditionalOnMissingBean(name = "ttlRedisCacheWriter")
    public RedisCacheWriter ttlRedisCacheWriter() {
        return RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ttlRedisCacheConfiguration")
    public RedisCacheConfiguration ttlRedisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(this.ttlSerializer()));
    }

    @Bean
    @ConditionalOnMissingBean(name = "ttlRedisCacheManager")
    public TtlRedisCacheManager ttlRedisCacheManager(RedisCacheWriter ttlRedisCacheWriter,
                                                     RedisCacheConfiguration ttlRedisCacheConfiguration) {
        return new TtlRedisCacheManager(ttlRedisCacheWriter, ttlRedisCacheConfiguration, new HashMap<>(16));
    }

    @Bean
    @ConditionalOnMissingBean(name = "ttlRedisCacheResolver")
    public TtlRedisCacheResolver ttlRedisCacheResolver(TtlRedisCacheManager ttlRedisCacheManager) {
        return new TtlRedisCacheResolver(ttlRedisCacheManager);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ttlRedisTemplate")
    public RedisTemplate<String, Object> ttlRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(this.ttlSerializer());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.java());
        return redisTemplate;
    }

    private RedisSerializer<?> ttlSerializer() {
        switch (serialProperties.getType()) {
            case json: return RedisSerializer.json();
            case string: return RedisSerializer.string();
            default: return RedisSerializer.java();
        }
    }

    @Bean
    @ConditionalOnClass(TaskProperties.class)
    @ConditionalOnProperty(prefix = CacheConstant.TASK_PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheTask cacheTask() {
        return new CacheTask();
    }

    @Bean("cacheRefresher")
    public CacheRefresher cacheRefresher() {
        return new CacheRefresher();
    }

    @Bean("cacheThreadPoolExecutor")
    @ConditionalOnMissingBean(value = {ThreadPoolExecutor.class, ExecutorService.class})
    public ThreadPoolExecutor cacheThreadPoolExecutor() {
        int poolSize = 2;
        int maxPoolSize = 16;
        if (taskProperties.getPoolSize() > 0) {
            poolSize = taskProperties.getPoolSize();
        }
        if (poolSize > maxPoolSize) {
            poolSize = maxPoolSize;
        }
        int queueSize = 32;
        int maxQueueSize = 1024;
        if (taskProperties.getQueueSize() > 0) {
            queueSize = taskProperties.getQueueSize();
        }
        if (queueSize > maxQueueSize) {
            queueSize = maxQueueSize;
        }
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(CacheConstant.THREAD_FACTORY);
        return new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize), threadFactory);
    }

    @Bean("springContextUtil")
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }

    @Bean
    @ConditionalOnMissingBean(name = "ttlCacheKeyGenerator")
    public TtlCacheKeyGenerator ttlCacheKeyGenerator() {
        return new TtlCacheKeyGenerator();
    }

    @Bean("cacheAccessRegistrar")
    public CacheAccessRegistrar cacheAccessRegistrar() {
        return new CacheAccessRegistrar();
    }
}
