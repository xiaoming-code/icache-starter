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

import java.io.Serializable;

/**
 * 缓存调用封装类
 * @author lzc
 * @date 2021/03/09 16:52
 */
public class CacheInvocation implements Serializable {

    private static final long serialVersionUID = 4622181904589844690L;

    private final String targetName;

    private final String methodName;

    private final Class<?>[] argTypes;

    private final Object[] args;

    private final String key;

    private final long ttl;

    public CacheInvocation(String targetName, String methodName, Class<?>[] argTypes, Object[] args, String key, long ttl) {
        this.targetName = targetName;
        this.methodName = methodName;
        this.argTypes = argTypes;
        this.args = args;
        this.key = key;
        this.ttl = ttl;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getArgTypes() {
        return argTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getKey() {
        return key;
    }

    public long getTtl() {
        return ttl;
    }
}
