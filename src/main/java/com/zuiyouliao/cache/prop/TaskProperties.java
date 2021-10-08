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
package com.zuiyouliao.cache.prop;

import com.zuiyouliao.cache.constant.CacheConstant;
import com.zuiyouliao.cache.constant.TimeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务信息配置
 * @author lzc
 * @date 2021/03/11 14:23
 */
@ConfigurationProperties(prefix = CacheConstant.TASK_PREFIX)
public class TaskProperties {

    /**
     * 是否开启定时任务，默认：true
     */
    private String enabled = "true";

    /**
     * 任务时间表达式
     */
    private String cron;

    /**
     * 任务线程池
     */
    private int poolSize = 2;

    /**
     * 任务线程池的队列
     */
    private int queueSize = 32;

    /**
     * 自刷缓存对应缓存数据的超期访问时间，0或负值表示不清理该自刷缓存
     */
    private long cleanOverAccessTime = 0L;

    /**
     * 自刷缓存对应缓存数据的超期访问时间单位，默认单位：DAYS（天）
     */
    private TimeUnit cleanOverAccessTimeUnit = TimeUnit.DAYS;

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public long getCleanOverAccessTime() {
        return cleanOverAccessTime;
    }

    public void setCleanOverAccessTime(long cleanOverAccessTime) {
        this.cleanOverAccessTime = cleanOverAccessTime;
    }

    public TimeUnit getCleanOverAccessTimeUnit() {
        return cleanOverAccessTimeUnit;
    }

    public void setCleanOverAccessTimeUnit(TimeUnit cleanOverAccessTimeUnit) {
        this.cleanOverAccessTimeUnit = cleanOverAccessTimeUnit;
    }
}
