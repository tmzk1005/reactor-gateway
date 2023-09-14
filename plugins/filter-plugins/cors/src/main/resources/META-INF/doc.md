# 功能

配置跨域策略

# 配置

此插件有比较多的参数：

- allowedOrigins
- allowedOriginPatterns
- allowedMethods
- allowedHeaders
- exposedHeaders
- allowCredentials
- maxAge

各个参数要如何配置，需要深入的了解"跨域"相关的背景知识，请参考 [https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

# 例子

```json
{
    "allowedOrigins": [
        "*"
    ],
    "allowedOriginPatterns": [
        "*"
    ],
    "allowedMethods": [
        "GET",
        "POST",
        "HEAD",
        "PUT",
        "DELETE",
        "PATCH",
        "OPTIONS",
        "TRACE"
    ],
    "allowedHeaders": [
        "*"
    ],
    "exposedHeaders": [],
    "allowCredentials": false,
    "maxAge": 0
}
```