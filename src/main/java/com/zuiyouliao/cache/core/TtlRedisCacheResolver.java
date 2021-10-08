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

import com.zuiyouliao.cache.annotation.TtlCacheable;
import com.zuiyouliao.cache.clone.CacheExpressionRootObject;
import com.zuiyouliao.cache.prop.ProjectProperties;
import com.zuiyouliao.cache.task.CacheInvocation;
import com.zuiyouliao.cache.task.CacheRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 自定义带ttl的redis缓存解析器
 *
 * @author lzc
 * @date 2021/03/08 16:42
 */
public class TtlRedisCacheResolver extends SimpleCacheResolver {

    /**
     * 仿造 AbstractCacheManager 创建的cacheMap，用于存储 ttl > 0 的RedisCache
     */
    private final ConcurrentMap<String, RedisCache> cacheMap = new ConcurrentHashMap<>(16);

    /**
     * 缓存最大容量
     */
    private static final int MAX_CACHE_SIZE = 1024;

    /**
     * SpEL表达式解析器
     */
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final Logger logger = LoggerFactory.getLogger(TtlRedisCacheResolver.class);

    @Resource(name = "ttlRedisCacheConfiguration")
    private RedisCacheConfiguration redisCacheConfiguration;

    @Resource(name = "ttlCacheKeyGenerator")
    private TtlCacheKeyGenerator ttlCacheKeyGenerator;

    @Resource
    private CacheRefresher cacheRefresher;

    @Resource
    private ProjectProperties projectProperties;

    public TtlRedisCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
    }

    /**
     * 重写 AbstractCacheResolver 的resolveCaches
     * @param context 缓存注解被拦截的上下文
     * @return java.util.Collection&lt;? extends org.springframework.cache.Cache&gt;
     * @author lzc
     * @date 2021/03/09 14:03
     */
    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<String> cacheNames = this.getCacheNames(context);
        if (cacheNames == null) {
            return Collections.emptyList();
        } else {
            Collection<Cache> result = new ArrayList<>(cacheNames.size());
            String prefix = Objects.equals("", projectProperties.getName()) ? "" : projectProperties.getName() + "::";
            for (String cacheName : cacheNames) {
                // ttl > 0，有过期时间
                cacheName = prefix + cacheName;
                Cache cache = this.getCache(context, cacheName);
                if (cache == null) {
                    // ttl == 0，无过期时间
                    cache = super.getCacheManager().getCache(cacheName);
                }
                if (cache == null) {
                    throw new IllegalArgumentException("Cannot find cache named '" + cacheName + "' for " + context.getOperation());
                }

                result.add(cache);
                this.markAutoRefreshCache(context, cacheName, result);
            }

            return result;
        }
    }

    /**
     * 获取缓存
     * @param context 缓存注解被拦截的上下文
     * @param cacheName 缓存名称
     * @return org.springframework.cache.Cache
     * @author lzc
     * @date 2021/03/10 14:52
     */
    private Cache getCache(CacheOperationInvocationContext<?> context, String cacheName) {
        TtlCacheable ttlCacheable = context.getMethod().getAnnotation(TtlCacheable.class);
        // 注解上的ttl > 0才执行
        if (ttlCacheable != null && ttlCacheable.ttl() > 0) {
            String cacheKey = cacheName + "-" + ttlCacheable.ttl();
            RedisCache cache = this.cacheMap.get(cacheKey);
            this.clearIfOverSize();
            if (cache == null) {
                synchronized(this.cacheMap) {
                    cache = cacheMap.get(cacheKey);
                    if (cache == null) {
                        cache = this.createRedisCache(cacheName, ttlCacheable.ttl());
                        if (cache != null) {
                            this.cacheMap.putIfAbsent(cacheKey, cache);
                        }
                    }
                }
            }
            return cache;
        }
        return null;
    }

    /**
     * 创建缓存
     * @param cacheName 缓存名称
     * @param ttl 过期时间
     * @return org.springframework.data.redis.cache.RedisCache
     * @author lzc
     * @date 2021/03/10 14:53
     */
    private RedisCache createRedisCache(String cacheName, Long ttl) {
        CacheManager cacheManager = super.getCacheManager();
        if (cacheManager instanceof TtlRedisCacheManager) {
            TtlRedisCacheManager manager = (TtlRedisCacheManager) cacheManager;
            RedisCacheConfiguration configuration = manager.getCacheConfiguration();
            return manager.createRedisCache(cacheName, configuration.entryTtl(Duration.ofSeconds(ttl)));
        }
        return null;
    }

    private void clearIfOverSize() {
        if (cacheMap.size() > MAX_CACHE_SIZE) {
            cacheMap.clear();
        }
    }

    /**
     * 标记需要自动刷新的缓存
     * @param context 缓存注解被拦截的上下文
     * @param cacheName 缓存名称
     * @param caches 缓存集合
     * @author lzc
     * @date 2021/03/10 14:54
     */
    private void markAutoRefreshCache(CacheOperationInvocationContext<?> context, String cacheName, Collection<Cache> caches) {
        Method method = context.getMethod();
        TtlCacheable ttlCacheable = method.getAnnotation(TtlCacheable.class);
        if (ttlCacheable == null || !ttlCacheable.autoRefreshWithoutUnless()) {
            return;
        }
        // 如果unless有设置，则禁止刷新，因为无法手动获取方法返回值
        if (!StringUtils.isEmpty(ttlCacheable.unless())) {
            logger.warn(String.format("由于@TtlCacheable的unless有值，将禁止自动刷新。cacheName=%s，key=%s", Arrays.toString(ttlCacheable.value()), ttlCacheable.key()));
            return;
        }

        Boolean conditional = this.parseCacheSpEL(caches, ttlCacheable.condition(), context.getTarget(), method, context.getArgs(), Boolean.class);
        if (conditional != null && !conditional) {
            return;
        }
        String key = this.parseCacheKey(caches, ttlCacheable, context.getTarget(), method, context.getArgs());
        String redisKey = this.getRedisKey(cacheName, key);
        CacheInvocation cacheInvocation = new CacheInvocation(context.getTarget().getClass().getName(), method.getName(),
                method.getParameterTypes(), context.getArgs(), redisKey, ttlCacheable.ttl());
        cacheRefresher.addCache(cacheInvocation);
    }

    /**
     * 解析缓存注解的SpEL表达式
     * @param caches 缓存集合
     * @param expression SpEL
     * @param target 目标类
     * @param method 模板方法
     * @param args 方法参数
     * @param resultType 返回值类型
     * @return T
     * @author lzc
     * @date 2021/03/10 14:48
     */
    @SuppressWarnings("unchecked")
    private <T> T parseCacheSpEL(Collection<Cache> caches, String expression, Object target, Method method, Object[] args, Class<T> resultType) {
        if (StringUtils.isEmpty(expression)) {
            if (resultType == String.class) {
                SimpleKey simpleKey = new SimpleKey(args);
                return (T) simpleKey.toString();
            }
            if (resultType == Boolean.class) {
                return (T) Boolean.TRUE;
            }
        }
        // 构建参数上下文
        CacheExpressionRootObject rootObject = new CacheExpressionRootObject(caches, method, args, target, target.getClass());
        EvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, args, new DefaultParameterNameDiscoverer());
        // 计算表达式
        return EXPRESSION_PARSER.parseExpression(expression).getValue(evaluationContext, resultType);
    }

    private String parseCacheKey(Collection<Cache> caches, TtlCacheable ttlCacheable, Object target, Method method, Object[] args) {
        if (StringUtils.isEmpty(ttlCacheable.keyGenerator()) || !Objects.equals("ttlCacheKeyGenerator", ttlCacheable.keyGenerator())) {
            return this.parseCacheSpEL(caches, ttlCacheable.key(), target, method, args, String.class);
        }
        return ttlCacheKeyGenerator.generate(target, method, args).toString();
    }

    /**
     * 获取redis缓存中的key
     * @param cacheName 缓存名称（注解中的名称）
     * @param cacheKey 缓存key（注解中的key）
     * @return java.lang.String
     * @author lzc
     * @date 2021/03/10 15:39
     */
    private String getRedisKey(String cacheName, String cacheKey) {
        return redisCacheConfiguration.getKeyPrefixFor(cacheName).concat(cacheKey);
    }

}
