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

import com.zuiyouliao.cache.prop.TaskProperties;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.Resource;

/**
 * 缓存任务
 * <p>之所以不用@Scheduled，是为了从外部配置文件设置定时周期</p>
 * @author lzc
 * @date 2021/03/11 11:24
 */
public class CacheTask implements SchedulingConfigurer {

    @Resource
    private TaskProperties taskProperties;

    @Resource
    private CacheRefresher cacheRefresher;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        if (taskProperties.getCron() != null && taskProperties.getCron().trim().length() > 0) {
            registrar.addCronTask(this::execute, taskProperties.getCron());
        }
    }

    private void execute() {
        // 先清理超期未访问缓存的对应自刷缓存，再进行自刷
        cacheRefresher.cleanRefreshValue();
        cacheRefresher.refresh();
    }

}
