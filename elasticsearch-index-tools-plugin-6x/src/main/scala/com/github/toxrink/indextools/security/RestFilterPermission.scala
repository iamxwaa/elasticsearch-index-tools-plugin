package com.github.toxrink.indextools.security

import com.github.toxrink.indextools.core.config.{IndexToolsSettings, SecurityConfig}
import com.github.toxrink.indextools.core.constants.{IndexToolsConstants, PermissionConstants}
import com.github.toxrink.indextools.core.model
import com.github.toxrink.indextools.core.model._
import com.github.toxrink.indextools.core.security.SecurityAction
import com.github.toxrink.indextools.core.tools.HttpTools
import org.apache.commons.lang.StringUtils
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.node.DiscoveryNodes
import x.utils.JxUtils

/**
  * Created by xw on 2019/10/15.
  */
object RestFilterPermission {
  private val logger = JxUtils.getLogger(RestFilterPermission.getClass)

  private var users = List[UserPermission]()

  private var isLoaded = false

  val okCheckResult = CheckResult(status = true, PermissionConstants.CHECK_RESULT_OK)

  def loadCluster(client: Client, indextoolsSettings: IndexToolsSettings, discoveryNodes: DiscoveryNodes): Unit = {
    import scala.collection.JavaConversions._
    val port = indextoolsSettings.settings.getAsInt("http.port", 9200)
    val localHost = indextoolsSettings.settings.get("network.host", "localhost")
    discoveryNodes.foreach(node => {
      logger.info(s"Send update permission task to ${node.getHostName}")
      var result = ""
      try {
        if ("127.0.0.1".equals(node.getHostName)
            || "localhost".equals(node.getHostName)
            || localHost.equals(node.getHostName)) {
          load(client)
          result = "true [local]"
        } else {
          result = HttpTools.sendAdminGetRequest(node.getHostName, port, "/_itools/right/load")(
            indextoolsSettings.securityConfig)
        }
      } catch {
        case e: Throwable =>
          logger.error(e)
          result = "false " + e.getMessage
      }
      logger.info(s"Update permission result(${node.getHostName}): $result")
    })
  }

  def load(client: Client): Unit = {
    logger.info("Load User Permission")
    val response = client
      .prepareSearch(IndexToolsConstants.USER_PERMISSION_INDEX_NAME)
      .setTypes(IndexToolsConstants.TYPE)
      .setFrom(0)
      .setSize(1000)
      .execute()
      .actionGet()

    val list = response.getHits.getHits
      .map(hit => {
        val map = hit.getSourceAsMap
        val name = map.get("username").asInstanceOf[String]
        val password = map.get("password").asInstanceOf[String]
        val adminright = map.getOrDefault("adminright", "").asInstanceOf[String]
        val dataright = map.getOrDefault("dataright", "").asInstanceOf[String]
        val settings = map.getOrDefault("settings", "").asInstanceOf[String]
        val indexPerm = if ("".equals(adminright)) {
          SecurityConfig.defaultAdminPermission
        } else {
          val create = adminright.contains("create")
          val delete = adminright.contains("delete")
          AdminPermission(create, delete)
        }
        val dataPerm = if ("".equals(dataright)) {
          SecurityConfig.defaultDataPermission
        } else {
          val write = dataright.contains("write")
          val read = dataright.contains("read")
          DataPermission(write, read)
        }
        var allowAllIndex = false
        val indexList = if (StringUtils.isEmpty(settings)) {
          List()
        } else {
          if ("*".equals(settings)) {
            allowAllIndex = true
            List()
          } else {
            settings
              .split("\n")
              .map(index => {
                val kv = index.split(":")
                val prefixMath = kv(0).endsWith("*")
                val prefixIndex = if (prefixMath) kv(0).substring(0, kv(0).length - 1) else ""
                if (kv.length > 1) {
                  IndexPermission(kv(0), kv(1).split(","), prefixMath, prefixIndex)
                } else {
                  IndexPermission(kv(0), Array(), prefixMath, prefixIndex)
                }
              })
              .toList
          }
        }
        model.UserPermission(superAdmin = false, name, password, indexPerm, dataPerm, indexList, allowAllIndex)
      })
      .toList
    isLoaded = true
    users = list
  }

  def checkUserPermission(client: Client, user: String, password: String): Option[UserPermission] = {
    if (!isLoaded) {
      load(client)
    }
    users.find(p => {
      p.name.equals(user) && p.password.equals(password)
    })
  }

  def checkPermission(implicit userAction: SecurityAction, action: String, request: ActionRequest): CheckResult = {
    var userPermission = userAction.getTransientPermission()
    if (userPermission.isEmpty) {
      userPermission = userAction.getContextUser()
    }
    if (userPermission.isDefined && userPermission.get.superAdmin) {
      userAction.setContextUser(userPermission)
      okCheckResult
    } else {
      if (action.startsWith(PermissionConstants.ACTION_INDEX_DATA_WRITE)) {
        checkRight(userPermission, userPermission => userPermission.data.write, PermissionConstants.NO_RIGHT_DATA_WRITE)
      } else if (action.startsWith(PermissionConstants.ACTION_INDEX_DATA_READ)) {
        val naf =
          checkRight(userPermission, userPermission => userPermission.data.read, PermissionConstants.NO_RIGHT_DATA_READ)
        if (naf.status) {
          request match {
            case r: SearchRequest =>
              val ck = checkIndexPermission(r.indices(), userPermission)
              CheckResult(ck._1, ck._2)
            case _ => okCheckResult
          }
        } else {
          naf
        }
      } else if (action.startsWith(PermissionConstants.ACTION_INDEX_CREATE)) {
        checkRight(userPermission,
                   userPermission => userPermission.admin.create,
                   PermissionConstants.NO_RIGHT_CREATE_INDEX)
      } else if (action.startsWith(PermissionConstants.ACTION_INDEX_DELETE)) {
        checkRight(userPermission,
                   userPermission => userPermission.admin.delete,
                   PermissionConstants.NO_RIGHT_DELETE_INDEX)
      } else {
        okCheckResult
      }
    }
  }

  private def checkRight(userPermission: Option[UserPermission], ok: UserPermission => Boolean, failMsg: String)(
      implicit userAction: SecurityAction): CheckResult = {
    if (userPermission.isEmpty) {
      CheckResult(status = false, PermissionConstants.CHECK_RESULT_NO_USER)
    } else {
      if (ok(userPermission.get)) {
        okCheckResult
      } else {
        CheckResult(status = false, failMsg)
      }
    }
  }

  def checkIndexPermission(indices: Array[String], permission: Option[UserPermission]): (Boolean, String) = {
    if (permission.get.allowAllIndex) {
      (true, PermissionConstants.PERMISSION_INDEX_OK)
    } else if (permission.get.indices.isEmpty) {
      (false, PermissionConstants.PERMISSION_NO_INDEX_CONFIG)
    } else {
      var failList = Array[String]()
      indices.foreach(index => {
        val check = permission.get.indices.exists(
          p2 => {
            if (p2.prefixMath) {
              index.startsWith(p2.prefixIndex)
            } else {
              index.equals(p2.index)
            }
          }
        )
        if (!check) {
          failList = failList ++ Array(index)
        }
      })
      if (failList.nonEmpty) {
        (false, PermissionConstants.PERMISSION_CAN_NOT_VISIT + " : " + failList.mkString(","))
      } else {
        checkIndexFieldsPermission(indices, permission)
      }
    }
  }

  def checkIndexFieldsPermission(indices: Array[String], permission: Option[UserPermission]): (Boolean, String) = {
    (true, PermissionConstants.PERMISSION_INDEX_FILED_OK)
  }
}
