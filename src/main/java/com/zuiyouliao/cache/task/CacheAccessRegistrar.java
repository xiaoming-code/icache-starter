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
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * 缓存访问记录器
 *
 * @author lzc
 * @date 2021/09/16 15:06
 */
public class CacheAccessRegistrar {

    @Resource(name = "ttlRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ProjectProperties projectProperties;

    /**
     * 登记缓存访问时间
     * @param cacheKey 缓存的key
     * @author lzc
     * @date 2021/09/16 15:18
     */
    public void register(String cacheKey) {
        redisTemplate.opsForHash().put(this.lastAccessKey(), cacheKey, System.currentTimeMillis());
    }

    private String lastAccessKey() {
        return CacheConstant.LAST_ACCESS +
                (Objects.equals("", projectProperties.getName()) ? "" : "::" + projectProperties.getName());
    }

    /**
     * 获取所有登记缓存
     *
     * @return java.util.Map&lt;java.lang.Object,java.lang.Object&gt;
     * @author lzc
     * @date 2021/09/16 15:30
     */
    public Map<Object, Object> getAllCaches() {
        return redisTemplate.opsForHash().entries(this.lastAccessKey());
    }

    /**
     * 清理指定缓存的访问时间
     *
     * @param cacheKey 缓存的key
     * @author lzc
     * @date 2021/09/16 15:32
     */
    public void delete(Object cacheKey) {
        redisTemplate.opsForHash().delete(this.lastAccessKey(), cacheKey);
    }
}
