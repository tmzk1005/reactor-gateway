# 功能

代理HTTP协议，将发送到网关的HTTP请求按照配置转发到真正提供服务的HTTP后端，支持自定义**负载均衡**策略或者**灰度发布**策略。

# 配置

此插件的配置主要有以下几个字段：

- upstreamEndpoint
- upstreamEndpointDecideFuncDef
- timeout

下面分别说明：

## upstreamEndpoint

此字段是一个字符串，若非空，则需要是一个合法的 `URI` 。配置要将请求路由到的上游HTTP服务URI，可以是一个静态值，例如 `http://abc.com/foo/bar` ，或者是包含有环境变量的动态值，例如 `http://{ERP_DOMAIN}/foo/bar` ，其中`{ERP_DOMAIN}` 引用了名为 `ERP_DOMAIN` 的环境变量。关于如何配置环境变量，参考环境变量相关的文档。通过此种方式，可以实现将同一个API发布到不同的环境时，将请求路由到不同的地址。

当字段配置为 `null` 或者空字符串时，表示需要由 `upstreamEndpointDecideFuncDef` 动态决定上游地址。当有值时，`upstreamEndpointDecideFuncDef` 将不起作用。

## upstreamEndpointDecideFuncDef

此功能将动态决定上游URI的权力完全下放给用户配置，用户可以通过自定义逻辑实现**负债均衡**，**灰度发布**，**A/B Test**等需求。

此字段是一段 `groovy` 代码 ，使用此字段时，需要将 `upstreamEndpoint` 设置为 `null` 或者空字符串。代码需要定义一个名为 `decideUri` 的 `groovy` 方法, 例如：

```groovy
def decideUri() {
    return new URI("http://abc.com/foo/bar")
}
```

方法没有参数，但是有几个内置的变量可以引用，可以使用这些上下文变量来实现动态决定URI的逻辑：

- requestHeaders

    请求头信息，类型 `Map<String, String>`

- clientRequestUriPath

    请求路径，字符串类型，不带有query参数部分

- clientRequestUriParameters

    请求URI上的Query参数，类型 `Map<String, List<String>>`

- environment

    环境变量，类型 `Map<String, String>`

## timeout

定义超时时间，既网关等待上游返回响应的最长等待时间，正整数，单位为秒。当为 `0` 时表示永不超时。若超时，网关将放弃等待上游响应，直接返回给客户端 `504`

# 例子

- 固定的上游地址

  ```json
  {
      "upstreamEndpoint": "http://abc.com/foo/bar",
      "upstreamEndpointDecideFuncDef": null,
      "timeout": 0
  }
  ```

- 引用了环境变量的上游地址

  ```json
  {
      "upstreamEndpoint": "http://{ERP_DOMAIN}foo/bar",
      "upstreamEndpointDecideFuncDef": null,
      "timeout": 60
  }
  ```

- 使用groovy脚本自定义决定URI的逻辑

  ```json
  {
      "upstreamEndpoint": null,
      "upstreamEndpointDecideFuncDef": "def decideUri() { ... }",
      "timeout": 30
  }
  ```

  > 需要自行实现名为 `decideUri` 的方法
