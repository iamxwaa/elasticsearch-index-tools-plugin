package com.github.toxrink.indextools.core.config

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import com.github.toxrink.indextools.core.constants.IndexToolsConstants
import x.utils.TimeUtils

/**
  * Created by xw on 2019/10/23.
  */
case class AutoCreateIndex(name: String,
                           nameFormat: String,
                           format: String,
                           aliasFormat: String,
                           timeField: String,
                           history: (Date, Date)) {

  def this(name: String,
           nameFormat: String,
           format: String,
           aliasFormat: String,
           timeField: String,
           startHistory: String,
           endHistory: String) = {
    this(
      name,
      nameFormat,
      format,
      aliasFormat,
      timeField, {
        val sdf = new SimpleDateFormat(format)
        (sdf.parse(startHistory), sdf.parse(endHistory))
      }
    )
  }

  val increaseType: (Short, Int) = getIncreaseType

  private def getPreCreateDate(date: Date): Date = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    increaseType._1 match {
      case 0 => cal.add(Calendar.MONTH, 1)
      case 1 => cal.add(Calendar.YEAR, 1)
      case 2 => cal.add(Calendar.YEAR, 1)
      case 3 => cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    cal.getTime
  }

  def index(date: Date, onlyName: Boolean): String = {
    if (onlyName) {
      name
    } else {
      nameFormat
        .replace(IndexToolsConstants.NAME_POSITION, name)
        .replace(IndexToolsConstants.TIME_POSITION, new SimpleDateFormat(format).format(getPreCreateDate(date)))
    }
  }

  def alias(now: Date): List[(String, (Date, Date))] = {
    val date = getPreCreateDate(now)
    val first = Calendar.getInstance()
    first.setTime(date)
    val last = Calendar.getInstance()
    last.setTime(date)
    //默认按天生成别名
    increaseType._1 match {
      case 0 =>
        first.setTime(TimeUtils.getFirstDayOfMonth(date))
        last.setTime(TimeUtils.getLastDayOfMonth(date))
      case 1 =>
        first.set(Calendar.MONTH, 0)
        //设置为1月第一天
        first.setTime(TimeUtils.getFirstDayOfMonth(first.getTime))
        last.set(Calendar.MONTH, 11)
        //设置为12月最后一天
        last.setTime(TimeUtils.getLastDayOfMonth(last.getTime))
      case 2 =>
        first.set(Calendar.MONTH, 0)
        //设置为1月第一天
        first.setTime(TimeUtils.getFirstDayOfMonth(first.getTime))
        last.set(Calendar.MONTH, 11)
        //设置为12月最后一天
        last.setTime(TimeUtils.getLastDayOfMonth(last.getTime))
      case 3 =>
      //不处理
    }

    if (first.getTime == last.getTime) {
      List()
    } else {
      var set = Set[(String, (Date, Date))]()
      set = set ++ Set(generateAlias(first.getTime))
      set = set ++ Set(generateAlias(last.getTime))
      while (first.before(last)) {
        set = set ++ Set(generateAlias(first.getTime))
        first.add(increaseType._2, 1)
      }
      set.toList
    }
  }

  /**
    * 获取时间增加类型
    *
    * @return
    * 0: 月索引,按天增加
    * 1: 年索引,按月增加
    * 2: 年索引,按天增加
    * -1: 未知
    */
  def getIncreaseType: (Short, Int) = {
    if (format.endsWith("MM") && aliasFormat.endsWith("dd")) {
      (0, Calendar.DAY_OF_MONTH)
    } else if (format.endsWith("yyyy") && aliasFormat.endsWith("MM")) {
      (1, Calendar.MONTH)
    } else if (format.endsWith("yyyy") && aliasFormat.endsWith("dd")) {
      (2, Calendar.DAY_OF_MONTH)
    } else if (format.endsWith("dd") && aliasFormat.endsWith("dd")) {
      (3, Calendar.DAY_OF_MONTH)
    } else {
      (-1, -1)
    }
  }

  private def generateAlias(date: Date): (String, (Date, Date)) = {
    val sdf = new SimpleDateFormat(aliasFormat)
    val aliasName =
      nameFormat
        .replace(IndexToolsConstants.NAME_POSITION, name)
        .replace(IndexToolsConstants.TIME_POSITION, sdf.format(date))

    val cal = Calendar.getInstance()
    if (Calendar.MONTH == increaseType._2) {
      cal.setTime(TimeUtils.getFirstDayOfMonth(date))
    } else {
      cal.setTime(date)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
    }
    val startTime = cal.getTime

    if (Calendar.MONTH == increaseType._2) {
      cal.setTime(TimeUtils.getLastDayOfMonth(date))
    } else {
      cal.set(Calendar.HOUR_OF_DAY, 23)
      cal.set(Calendar.MINUTE, 59)
      cal.set(Calendar.SECOND, 59)
      cal.set(Calendar.MILLISECOND, 999)
    }

    val endTime = cal.getTime

    (aliasName, (startTime, endTime))
  }
}
