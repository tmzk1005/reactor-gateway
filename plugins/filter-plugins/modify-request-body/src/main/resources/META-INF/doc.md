# 功能

修改请求体。在网关将请求转发到上游之前，可以修改请求体内容。

# 配置

使用此插件，用户需要使用 `groovy` 实现字节数组到字节数组的转换逻辑。配置的字段名称为 `convertFuncDef` 。值为一段 `groovy` 代码，在代码中需要定义一个名为 `convert` 的函数。

例如：

函数定义：
```groovy
def convert() { 
    return rawRequestBody 
}
```

> 这个例子中没有实际有用的逻辑，只是把原始的请求内容返回了。

配置：
```json
{
    "convertFuncDef": "def convert() { return rawRequestBody }"
}
```

`convert` 函数没有参数，这样方便用户书写。原始的请求体内容，可以直接引用名为 `rawRequestBody` 的变量，其值类型为字节数组。另外，考虑到转换的逻辑也有可能需要用到请求头信息，`convert` 函数中也可以通过名为 `requestHeaders` 的变量引用请求头信息，其类型为 `Map<String, List<String>>` 。

`convert` 函数的返回值应该是字节数组，返回结果将作为新的请求体内容发往上游。另外，为了简化用户开发 `convert` 函数，也支持返回字符串和普通Java对象，插件会将字符串按 `UTF-8` 编码转化为字节数组，如果是普通Java对象，插件会先将对象序列化为Json字符串，然后按 `UTF-8` 编码转化为字节数组。
