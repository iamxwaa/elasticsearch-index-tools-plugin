package com.github.toxrink.indextools.core.constants;

/**
 * Created by xw on 2019/11/4.
 */
public interface PermissionConstants {
    String ACTION_INDEX_DATA_WRITE = "indices:data/write";
    String ACTION_INDEX_DATA_READ = "indices:data/read";
    String ACTION_INDEX_CREATE = "indices:admin/create";
    String ACTION_INDEX_DELETE = "indices:admin/delete";

    String CHECK_RESULT_OK = "OK";
    String CHECK_RESULT_NO_USER = "未获取到用户信息";

    String PERMISSION_INDEX_OK = "OK";
    String PERMISSION_INDEX_FILED_OK = "OK";
    String PERMISSION_NO_INDEX_CONFIG = "未配置索引权限";
    String PERMISSION_CAN_NOT_VISIT = "无权访问以下索引";

    String NO_RIGHT_DATA_WRITE = "无数据写入权限";
    String NO_RIGHT_DATA_READ = "无数据读取权限";
    String NO_RIGHT_CREATE_INDEX = "无索引创建权限";
    String NO_RIGHT_DELETE_INDEX = "无索引删除权限";

    String WRONG_RIGHT = "权限错误";
}
