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

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * 缓存key生成器
 * @author lzc
 * @date 2021/03/16 15:21
 */
public class TtlCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... args) {
        StringJoiner joiner = new StringJoiner("-");
        joiner.add(target.getClass().getSimpleName());
        joiner.add(method.getName());
        if (args == null || args.length == 0) {
            return joiner.toString();
        }
        Object[] sortedArgs = args.clone();
        int hash = Arrays.deepHashCode(sortedArgs);
        joiner.add(String.valueOf(hash));
        return joiner.toString();
    }
}
