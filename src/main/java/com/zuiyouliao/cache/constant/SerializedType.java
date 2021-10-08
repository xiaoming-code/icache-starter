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
 * 缓存值的序列化类型，支持：json（默认）、jdk（返回值需要实现java.io.Serializable接口）和string
 *
 * @author lzc
 * @date 2021/04/28 10:24
 */
public enum SerializedType {

    /**
     * jdk；默认；返回值需要实现java.io.Serializable接口
     */
    jdk,
    /**
     * json
     */
    json,
    /**
     * 字符串
     */
    string;
}
