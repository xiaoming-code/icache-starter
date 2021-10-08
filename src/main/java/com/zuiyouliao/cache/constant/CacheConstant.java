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
package com.zuiyouliao.cache.constant;

/**
 * 常量
 * @author lzc
 * @date 2021/03/10 16:07
 */
public final class CacheConstant {

    private CacheConstant() {}

    public static final String PROJECT_CONSTANT = "z-cache";

    public static final String REFRESH_KEY = PROJECT_CONSTANT + "::refresh";

    public static final String TASK_PREFIX = PROJECT_CONSTANT + ".task";

    public static final String THREAD_FACTORY = PROJECT_CONSTANT + "-thread-pool-";

    public static final String SERIAL_PREFIX = PROJECT_CONSTANT + ".serializer";

    public static final String PROJECT_PREFIX = PROJECT_CONSTANT + ".project";

    public static final String LAST_ACCESS = PROJECT_CONSTANT + "::last-access";

}
