# 项目说明
该项目基于Spring的@Cacheable和Redis，通过自定义注解@TtlCacheable，实现缓存的存活时间配置和自动刷新。

#### 使用说明
@TtlCacheable用法和@Cacheable一样，只是额外加了两个属性：
* ttl，可配置缓存存活时间；
* autoRefreshWithoutUnless，实现自动刷新（原本想命名为autoRefresh，
  但由于无法同时实现unless和自动刷新，故改名）。

引入该依赖的项目，可以在application.properties文件中配置自动刷新任务相关数据。
* z-cache.task.enabled = true // 是否开启自动刷新，默认true，设置为false时autoRefreshWithoutUnless不起作用
* z-cache.task.cron = cron表达式 // 自动刷新任务的周期时间，当z-cache.task.enabled = true时必须设置
* z-cache.task.poolSize = 2 // 自动刷新任务线程池可用线程数量，默认2；如果有自定义线程池，将使用自定义的
* z-cache.task.queueSize = 32 // 自动刷新任务线程池可用队列大小，默认32；如果有自定义线程池，将使用自定义的
* z-cache.task.clean-over-access-time = 0L // 自刷缓存对应缓存数据的超期访问时间，0或负值表示不清理该自刷缓存
* z-cache.task.clean-over-access-time-unit = days // 自刷缓存对应缓存数据的超期访问时间单位，默认单位：DAYS（天）

其它配置
* z-cache.serializer.type = jdk // 缓存值序列化，jdk（默认，需要实现java.io.Serializable接口）、json和string
* z-cache.project.name = "" // 项目名（用于隔离环境），无默认值，需要的话自行配置

<b>关于序列化</b>：即使z-cache.serializer.type配置为json或string，用于刷新的方法参数依然是用jdk序列化

查找相关缓存
* 自刷缓存的key是 z-cache::refresh[::z-cache.project.name所填的项目名]
* 缓存访问记录时间的key是 z-cache::last-access[::z-cache.project.name所填的项目名]

支持SpringBoot项目自动装配，轻松上手。