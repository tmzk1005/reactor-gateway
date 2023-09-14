# 功能

在请求路由状态到上游之前，修改请求头信息，可以删除指定的头，新增头信息。

# 配置

- removeHeaderNames

  要删除的头，字符串数组，每个元素需要是一个合规的HTTP请求头名称。可以为空。

- addHeaders

  要新增的头信息，为0到多个键值对，数据结构为 `Map<String, String>`

removeHeaderNames和addHeaders都可以为空，但是不能都为空，否则此插件没有任何作用。

# 例子

```json
{
    "removeHeaderNames": [
        "Host"
    ],
    "addHeaders": {
        "X-SRC-SYS": "ERP"
    }
}
```
