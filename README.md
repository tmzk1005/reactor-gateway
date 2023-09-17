# Reactor-Gateway

`Reactor-Gateway`是一个基于[Reactor-Netty](https://github.com/reactor/reactor-netty)实现的**响应式**API网关。`Reactor-Gateway`主要是作为一个**业务型**的API网关来设计的，和流量型的API网关有一定的区别。

常用的微服务网关[Spring-Cloud-Gateway](https://github.com/spring-cloud/spring-cloud-gateway)是基于`Spring-Boot`和`Spring-WebFlux`的，而`Spring-WebFlux`的底层也是基于`Reactor-Netty`。此网关借鉴了一些`Spring-Cloud-Gateway`中比较好的设计思想，但是不使用任何Spring相关的框架，而是直接基于`Reactor-Netty`来实现，安装包的大小，资源消耗，性能等方面要优于`Spring-Cloud-Gateway`。

[官方文档](http://rgw.kanng.cn/)

此网关最主要的特点是:

## 1. 完全**插件化**的设计

网关的核心只是设计和实现了一套插件运行时（plugin runtime），以及一套开发插件的SDK-API。所有的http请求代理逻辑均由插件来实现。系统自带（built in）了一些常用的插件，包括但是不限于：

1. Http-Mock插件，用于开发阶段方便的Mock数据

2. Http1.1协议代理插件，最常用，最核心的插件，支持自定义负载均衡策略，自定义灰度发布策略。

3. IP黑白名单插件

4. 请求头修改插件

5. 请求体修改插件，可以用于协议转换

6. 响应头修改插件

7. 响应体修改插件，可以用于协议转换

8. 熔断

9. 限流

10. 跨域策略插件

11. WebSocket代理

12. gRPC代理，将后台的gRPC接口转换暴露为RestFul接口

13. App认证插件，API必须使用APP订阅后才能调用，支持请求防篡改

14. 访问日志插件，可以开启访问日志审计，请求方法，请求路径，请求头，请求体，响应码，响应头，响应体，请求耗时时间，上下行流量大小，客户端IP等等各种请求相关的信息都可以审计下来，并且支持过滤查询和统计分析。

若有非常特殊化的需求，内置的插件不能满足，可以使用开发插件的SOK-API自行开发插件，然后上传安装使用。

## 2. 方便快捷的可视化UI

`Reactor-Gateway`的子模块`Dashboard`实现了可视化的API生命周期管理，API的新建，发布，更新，下线等均可以通过可视化的UI非常方便的操作。API的处理逻辑通过拖拽想要使用的插件即可设计完成，插件的先后顺序也通过拖拽来排序。

每一个发布到的API后续被调用时，都会有访问日志记录，访问日志可以配置是否记录下请求体和响应体内容。在开发阶段非常方便上下游（调用方和服务方）联调。访问日志可以用于离线统计分析，可以同折线图的方式查看最近一段时间的调用情况统计，延时情况如何，以及上下行流量大小，非200的响应码个数等。

## 3. API文档管理

API的发布者在发布API时，可以为API编写文档（用markdown格式）。这样调用者可以通过学习文档来了解API的功能作用，以及如何调用API。

## 4. 多环境管理

可以设置多套环境，比如常见的3套环境：开发环境，测试环境，生产环境。也可以自定义更多环境，比如使用地理位置来区分：深圳生产环境，北京生产环境等。同一个API可以发布到不同的环境，不同的环境可以设置不同的环境变量，来影响同一个API在不同的环境的效果。

# 快速开始

## 安装

### tgz格式按转包下载

暂未提供

### Docker镜像

暂未提供

### Compose

暂未提供

### 源码编译

推荐在Linux环境下克隆源码后自行编译，需要:

1. OpenJDK-17或以上
2. Gradle 8.3或以上

>  推荐Debian11或以上，Ubuntu22.04或以上的环境。这样可以使用项目dev目录下的脚本快速下载和启动mongodb和kafka，进而运行起一个可以体验功能的环境。

#### clone此仓库

```bash
git clone https://github.com/tmzk1005/reactor-gateway.git
```

#### clone前端UI代码

前端代码放在单独的仓库[reactor-gateway-ui](https://github.com/tmzk1005/reactor-gateway-ui)，需要将此项目clone并放在`reactor-gateway`目录下，以便作为一个整体gradle项目进行编译构建。

```bash
cd reactor-gateway
git clone https://github.com/tmzk1005/reactor-gateway-ui.git
```

#### 编辑构建

如果是第一次运行，要先执行一个生成代码格式化规则的gradle task：

```bash
./gradlew fmtCreateRulesFile
```

然后构建，

```bash
./gradlew :release:tgz
```

如果是第一次，可能会耗时比较久，因为除了后端需要下载一些jar包依赖，前端项目的构建也需要下载和安装临时的`node`和`npm`，并安装前端依赖。请耐心等待，若因网络原因中断，重试即可。

构建成功后的安装包位于`release/build`目录下，文件名为`reactor-gateway-x.y.z.tgz`，`x.y.z`是版本号。解压压缩文件到想要按转的目录即可。

## 运行

运行软件，需要:

1. 启动mongodb

    生产环境需要自行安装部署高可用的mongodb集群，若是快速体验，可以用如下的命令快速启动一个使用默认配置的单机mongodb

    在**代码仓库**目录执行：
    ```bash
   ./dev/run-mongodb.sh
    ```

2. 启动kafka

   生产环境需要自行安装部署高可用的kafka集群，若是快速体验，可以用如下的命令快速启动一个使用默认配置的单机kafka

    在**代码仓库**目录执行：
    ```bash
    ./dev/run-run-kafka.sh
   ```

3. 使用默认的配置启动dashboard

    在解压安装目录执行
    ```bash
   bin/rgw-dashboard.sh
    ```

4. 启动gateway

    gateway启动后会向dashboard注册，并上报自己所属的环境id，因此启动gateway之前需要登陆到`dashboard`得到环境id。

   `dashboard`登陆地址`http://127.0.0.1:7000`，内置的体验用户名为 `rgw1`， 密码为 `rgw1@rgw`。管理员用户名称为 `admin`, 密码为 `admin@rgw`。登陆后点击环境菜单，即可得到内置的3个环境的id。

    启动开发环境gateway:

    ```bash
    bin/rgw-gateway.sh --environment.id=65052cf6f5bdd44467ed05c8 --server.port=8000
    ```

    若要体验多环境的效果，可以启动测试环境和生产环境：

    用一个不同的端口，启动测试环境gateway:

    ```bash
    bin/rgw-gateway.sh --environment.id=65052cf6f5bdd44467ed05c9 --server.port=8001
    ```

    再用一个不同的端口，启动生产环境gateway:
    ```
    bin/rgw-gateway.sh --environment.id=65052cf6f5bdd44467ed05ca --server.port=8002
    ```

5. 启动access-log-consumer

    ```bash
    bin/rgw-gateway.sh --environments=65052cf6f5bdd44467ed05c8,65052cf6f5bdd44467ed05c9,65052cf6f5bdd44467ed05ca
    ```

后续可参考官方文档，登陆到dashboard，执行API的新建，配置，发布，下线等操作。
