package com.github.toxrink.indextools.core.security

import java.security.{AccessController, Permission, PrivilegedAction}
import java.util

import com.google.gson.Gson

/**
  * Created by xw on 2019/9/6.
  */
object GrantRun {
  try {
    val perm = this.getClass.getClassLoader.loadClass("org.elasticsearch.SpecialPermission").newInstance()
    val sm = System.getSecurityManager()
    if (sm != null) {
      sm.checkPermission(perm.asInstanceOf[Permission])
    }
  } catch {
    case _: ClassNotFoundException =>
  }

  def apply[T](f: () => T): T = {
    AccessController.doPrivileged(new PrivilegedAction[T] {
      override def run(): T = {
        f()
      }
    })
  }

  def applyJava[T](p: PrivilegedAction[T]): T = {
    AccessController.doPrivileged(p)
  }

  def toJSON(data: util.Map[String, Object]): String = {
    AccessController.doPrivileged(new PrivilegedAction[String] {
      override def run(): String = {
        new Gson().toJson(data)
      }
    })
  }

  def parseJSON[T](json: String, clazz: Class[T]): T = {
    AccessController.doPrivileged(new PrivilegedAction[T] {
      override def run(): T = {
        new Gson().fromJson(json, clazz)
      }
    })
  }
}
