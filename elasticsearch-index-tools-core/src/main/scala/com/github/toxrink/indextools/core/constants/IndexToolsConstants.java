package com.github.toxrink.indextools.core.constants;

/**
 * Created by xw on 2019/10/21.
 */
public interface IndexToolsConstants {
    String USER_PERMISSION_INDEX_NAME = ".index-tools-user-permission";
    String AUTO_CREATE_INDEX_NAME = ".index-tools-auto-create";
    String ALIAS_RECORD_INDEX_NAME = ".index-tools-auto-create-record";

    String INDEX_GENERATETEST_POOL = ".index-tools-generatetest-pool";
    String INDEX_GENERATETEST_TEMPLATE = ".index-tools-generatetest-template";

    String TYPE = "doc";

    String USER_PERMISSION = "USER_PERMISSION_AAA_BBB_CCC";
    String USER_PERMISSION_USERNAME = "USER_PERMISSION_USERNAME";
    String USER_PERMISSION_PASSWORD = "USER_PERMISSION_PASSWORD";

    //索引名称时间占位符
    String TIME_POSITION = "<TIME>";
    String NAME_POSITION = "<NAME>";
    String NAME_FORMAT = NAME_POSITION + "-" + TIME_POSITION;
    String TIME_FIELD = "time";

    String INDEX_CREATE_JOB = "index_create_job";

    //-----------------以下为elasticsearch.yml中的配置-----------------

    String ITOOLS_AUTO_CREATE_OPEN = "itools.auto.create.open";
    String ITOOLS_AUTO_CREATE_AVALIABLE_INDEX = "itools.auto.create.avaliable.index";
    String ITOOLS_AUTO_CHECK_TIME = "itools.auto.check.time";
    String ITOOLS_AUTO_CREATE_INDEX_PREFIX = "itools.auto.create.index.";
    String ITOOLS_AUTO_CREATE_INDEX_NAME = "name";
    String ITOOLS_AUTO_CREATE_INDEX_NAME_FORMAT = "name.format";
    String ITOOLS_AUTO_CREATE_INDEX_FORMAT = "format";
    String ITOOLS_AUTO_CREATE_INDEX_ALIAS_FORMAT = "alias.format";
    String ITOOLS_AUTO_CREATE_INDEX_TIME_FIELD = "time.field";
    String ITOOLS_AUTO_CREATE_INDEX_CREATE_HISTORY = "create.history";

    String ITOOLS_TEMPLATES = "itools.templates";
    String ITOOLS_RESOURCE = "itools.resource";

    String ITOOLS_SECURITY_BASIC = "itools.security.basic.enabled";
    String ITOOLS_SECURITY_USERNAME = "itools.security.basic.username";
    String ITOOLS_SECURITY_PASSWORD = "itools.security.basic.password";

    String ITOOLS_SECURITY_HTTP_SSLONLY = "itools.security.http.ssl.enabled";
    String ITOOLS_SECURITY_HTTP_KEYSTORE = "itools.security.http.ssl.keystore.path";
    String ITOOLS_SECURITY_HTTP_KEYSTORE_PASSWORD = "itools.security.http.ssl.keystore.password";
    String ITOOLS_SECURITY_HTTP_CERTIFICATE_PASSWORD = "itools.security.http.ssl.certificate.password";
    String ITOOLS_SECURITY_HTTP_ALGORITHM = "itools.security.http.ssl.algorithm";
    String ITOOLS_SECURITY_HTTP_PROTOCOL = "itools.security.http.ssl.protocol";

    String ITOOLS_AUTO_CREATE_TESTDATA = "itools.auto.create.testdata.open";
    String ITOOLS_AUTO_CREATE_TESTDATA_CHECK_TIME = "itools.auto.create.testdata.time";
}
