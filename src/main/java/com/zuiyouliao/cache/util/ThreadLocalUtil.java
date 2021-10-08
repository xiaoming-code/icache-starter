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
package com.zuiyouliao.cache.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lzc
 * @date 2021/09/16 14:25
 */
public class ThreadLocalUtil {

    private static ThreadLocal<Map<String, Object>> THREAD_PARAMS = ThreadLocal.withInitial(HashMap::new);

    private static Map<String, Object> get() {
        return THREAD_PARAMS.get();
    }

    public static Object get(String key) {
        Map<String, Object> map = get();
        return map.get(key);
    }

    public static Object remove(String key) {
        Map<String, Object> map = get();
        return map.remove(key);
    }

    public static Object put(String key, Object value) {
        Map<String, Object> map = get();
        return map.put(key, value);
    }

}
