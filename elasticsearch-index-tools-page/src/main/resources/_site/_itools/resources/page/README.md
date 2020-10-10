# index-tools 使用说明

## 安装

- es集群每台机器执行以下命令安装插件

```bash
bin/elasticsearch-plugin install file:///path/to/elasticsearch-index-tools-plugin-***.zip
```

- 安装过程中提示获取权限,输入"y"回车即可

## 卸载

- es集群每台机器执行以下命令卸载插件

```bash
bin/elasticsearch-plugin remove index-tools
```

## 配置文件说明

- config/elasticsearch.yml 根据需要添加以下配置
- 如果启用https访问,参考以下命令生成证书文件

> %JAVA_HOME%/bin/keytool -genkey -alias indextools -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass 123456 -storepass 123456 -keystore D:/indextools.jks

```yml
#-------------定时创建测试数据----------------
##开启自动创建测试数据
itools.auto.create.testdata.open: true
#执行时间
itools.auto.create.testdata.time: "0 0 6 * * ?"
#-------------定时预创建索引配置----------------
#是否开启自动预创建索引
itools.auto.create.open: false
#预创建索引执行时间
itools.auto.check.time: "0 0 22 * * ?"
#配置格式itools.auto.create.index.<序号>.<属性名称>
#预创建索引有效的序号列表
itools.auto.create.avaliable.index: [1,2,3]
#预创建索引名称
itools.auto.create.index.1.name: mm2dd
#预创建索引名称格式（默认<NAME>-<TIME>）
itools.auto.create.index.1.name.format: <NAME>-with-<TIME>
#预创建索引时间格式
itools.auto.create.index.1.format: yyyy.MM
#预创建索引别名格式
itools.auto.create.index.1.alias.format: yyyy.MM.dd
#预创建时间过滤字段（默认为time）
itools.auto.create.index.1.time.field: time
#历史索引创建（不配置则不创建）
itools.auto.create.index.1.create.history: ["2019.01","2019.10"]

itools.auto.create.index.2.name: yyyy2mm
itools.auto.create.index.2.format: yyyy
itools.auto.create.index.2.alias.format: yyyy.MM
itools.auto.create.index.2.create.history: ["2018","2019"]

itools.auto.create.index.3.name: yyyy2dd
itools.auto.create.index.3.format: yyyy
itools.auto.create.index.3.alias.format: yyyy.MM.dd

#-------------启动加载的模板目录或文件路径----------------
#模板文件格式参考es官网格式,文件后缀以.json结尾
itools.templates: D:\\Downloads\\elasticsearch-6.1.3\\config\\templates

#-------------https单向认证配置----------------
#https开关
itools.security.http.ssl.enabled: true
#keystore路径
itools.security.http.ssl.keystore.path: D:\\Downloads\\elasticsearch-6.1.3\\config\\indextools.keystore
#keystore密码
itools.security.http.ssl.keystore.password: "123456"
#certificate密码
itools.security.http.ssl.certificate.password: "123456"
#加密算法,默认SunX509
itools.security.http.ssl.algorithm: SunX509
#安全协议,默认TLSv1.2
itools.security.http.ssl.protocol: TLSv1.2

#-------------是否启用Basic账号认证----------------
#basic认证开关
itools.security.basic.enabled: true
#管理员账号名称
itools.security.basic.username: admin
#管理员账号密码
itools.security.basic.password: admin
```

## 页面访问

- 启动 elasticsearch
- http访问 <http://ip:port/_itools>
- https访问 <https://ip:port/_itools>

![avatar](/_itools/resources/assets/img/readme1.png)

## Java Rest API 访问 https

> http方式访问参考官网代码

- 创建RestClientBuilder时设置shcema为https

```java
RestClientBuilder builder = RestClient.builder(Arrays.asList(config.getIpArr()).stream().map(m -> {
    return new HttpHost(m, port, "https"); //schema设置为https
}).collect(Collectors.toList()).toArray(new HttpHost[0]));
```

- 如果开启basic需添加账号密码认证

```java
final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials("username", "password")); //设置账号密码
builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
    @Override
    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
        httpClientBuilder.disableAuthCaching();
        //如果启用https需要配置sslcontext
        buildHttps(httpClientBuilder);
        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    }
});
```

- 如果启用https需要配置sslcontext

```java
private void buildHttps(HttpAsyncClientBuilder httpClientBuilder) {
    //信任任何证书
    TrustStrategy trustStrategy = new TrustStrategy() {
        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            return true;
        }
    };
    SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
    httpClientBuilder.setSSLContext(sslContext);
    //不对证书中的域名做任何处理
    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
}
```
