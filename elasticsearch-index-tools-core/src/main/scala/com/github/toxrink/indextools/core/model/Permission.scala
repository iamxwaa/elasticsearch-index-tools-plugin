package com.github.toxrink.indextools.core.model

/**
  * Created by xw on 2019/10/23.
  */
case class UserPermission(superAdmin: Boolean,
                          name: String,
                          password: String,
                          admin: AdminPermission,
                          data: DataPermission,
                          indices: List[IndexPermission],
                          allowAllIndex: Boolean) {}
case class AdminPermission(create: Boolean, delete: Boolean)
case class DataPermission(write: Boolean, read: Boolean)
case class IndexPermission(index: String, fields: Array[String], prefixMath: Boolean, prefixIndex: String)
//权限校验结果
case class CheckResult(status: Boolean, reason: String)
