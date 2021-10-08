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
package com.zuiyouliao.cache.task;

import com.zuiyouliao.cache.constant.CacheConstant;
import com.zuiyouliao.cache.prop.ProjectProperties;
import com.zuiyouliao.cache.prop.TaskProperties;
import com.zuiyouliao.cache.util.SpringContextUtil;
import com.zuiyouliao.cache.util.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存刷新器
 * @author lzc
 * @date 2021/03/11 16:00
 */
public class CacheRefresher {

    @Resource(name = "ttlRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ThreadPoolExecutor cacheThreadPoolExecutor;

    @Resource
    private ProjectProperties projectProperties;

    @Resource
    private CacheAccessRegistrar cacheAccessRegistrar;

    @Resource
    private TaskProperties taskProperties;

    private final Logger logger = LoggerFactory.getLogger(CacheRefresher.class);

    /**
     * 添加需要自动刷新的缓存
     * @param cacheInvocation 缓存调用对象
     * @author lzc
     * @date 2021/03/10 16:21
     */
    public void addCache(CacheInvocation cacheInvocation) {
        // 外部请求，非自动刷新时的反射调用
        if (ThreadLocalUtil.get(CacheConstant.REFRESH_KEY) == null) {
            // 记录访问缓存的时间
            cacheAccessRegistrar.register(cacheInvocation.getKey());
        }
        Object cache = redisTemplate.opsForHash().get(this.refreshKey(), cacheInvocation.getKey());
        CacheInvocation oldInvocation = (CacheInvocation) cache;
        if (oldInvocation != null && oldInvocation.getTtl() == cacheInvocation.getTtl()) {
            // 以key为准，如果过期时间一样，则视为完全一样，避免每次获取方法缓存都设置一次刷新参数的缓存
            return;
        }
        redisTemplate.opsForHash().put(this.refreshKey(), cacheInvocation.getKey(), cacheInvocation);
    }

    public void refresh() {
        Map<Object, Object> caches = redisTemplate.opsForHash().entries(this.refreshKey());
        if (caches.size() == 0) {
            return;
        }
        for (Map.Entry<Object, Object> entry : caches.entrySet()) {
            try {
                final CacheInvocation cacheInvocation = (CacheInvocation) entry.getValue();
                if (cacheInvocation == null) {
                    redisTemplate.opsForHash().delete(this.refreshKey(), entry.getKey());
                    continue;
                }
                cacheThreadPoolExecutor.execute(() -> this.execute(cacheInvocation));
            } catch (ClassCastException e) {
                redisTemplate.opsForHash().delete(this.refreshKey(), entry.getKey());
            }
        }
    }

    private void execute(CacheInvocation cacheInvocation) {
        try {
            ThreadLocalUtil.put(CacheConstant.REFRESH_KEY, Boolean.TRUE);
            // 删除缓存，不然缓存未过期的情况，反射请求方法得到的结果是缓存
            redisTemplate.delete(cacheInvocation.getKey());
            Class<?> targetClass = Class.forName(cacheInvocation.getTargetName());
            Object target = SpringContextUtil.getBean(targetClass);
            Method method = targetClass.getMethod(cacheInvocation.getMethodName(), cacheInvocation.getArgTypes());
            Object data = method.invoke(target, cacheInvocation.getArgs());
            if (cacheInvocation.getTtl() > 0) {
                redisTemplate.opsForValue().set(cacheInvocation.getKey(), data, cacheInvocation.getTtl(), TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(cacheInvocation.getKey(), data);
            }
        } catch (Exception e) {
            logger.error("CacheInvocation reflect fail", e);
            // 删除无法反射的自刷缓存
            redisTemplate.opsForHash().delete(this.refreshKey(), cacheInvocation.getKey());
        } finally {
            ThreadLocalUtil.remove(CacheConstant.REFRESH_KEY);
        }
    }

    private String refreshKey() {
        return CacheConstant.REFRESH_KEY +
                (Objects.equals("", projectProperties.getName()) ? "" : "::" + projectProperties.getName());
    }

    /**
     * 超期未访问接口缓存，删掉对应自刷缓存和访问时间缓存
     *
     * @author lzc
     * @date 2021/09/16 16:45
     */
    public void cleanRefreshValue() {
        long overAccessTime = taskProperties.getCleanOverAccessTime();
        com.zuiyouliao.cache.constant.TimeUnit overAccessTimeUnit = taskProperties.getCleanOverAccessTimeUnit();
        if (overAccessTime <= 0 || overAccessTimeUnit == null) {
            return;
        }

        Map<Object, Object> caches = cacheAccessRegistrar.getAllCaches();
        if (caches.size() == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<Object, Object> entry : caches.entrySet()) {
            try {
                final Long accessMillisTime = (Long) entry.getValue();
                if (accessMillisTime == null) {
                    redisTemplate.opsForHash().delete(this.refreshKey(), entry.getKey());
                    cacheAccessRegistrar.delete(entry.getKey());
                    continue;
                }
                // 超期未访问接口缓存，删掉对应自刷缓存和访问时间缓存
                long overTime = overAccessTimeUnit.toMillis(overAccessTime);
                if (now - accessMillisTime > overTime) {
                    redisTemplate.opsForHash().delete(this.refreshKey(), entry.getKey());
                    cacheAccessRegistrar.delete(entry.getKey());
                }
            } catch (ClassCastException e) {
                redisTemplate.opsForHash().delete(this.refreshKey(), entry.getKey());
                cacheAccessRegistrar.delete(entry.getKey());
            }
        }
    }
}
