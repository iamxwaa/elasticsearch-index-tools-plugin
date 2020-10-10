package com.github.toxrink.indextools.core.quartz

import java.util.Properties

import org.quartz._
import org.quartz.impl.triggers.CronTriggerImpl
import org.quartz.impl.{JobDetailImpl, StdSchedulerFactory}
import x.utils.JxUtils

/**
  * Created by xw on 2019/12/23.
  */
case class EsScheduler() {
  private val logger = JxUtils.getLogger(EsScheduler.getClass)

  private val stdSchedulerFactory = new StdSchedulerFactory

  private var scheduler: Scheduler = _

  def init(prop: Properties): Unit = {
    stdSchedulerFactory.initialize(prop)
    scheduler = stdSchedulerFactory.getScheduler
  }

  def addSchedule(name: String, cron: String, jobClass: Class[_ <: Job], jobDataMap: JobDataMap): Unit = {
    val job = new JobDetailImpl
    job.setKey(JobKey.jobKey("jobkey:" + name))
    job.setName("jobname:" + name)

    val trigger = new CronTriggerImpl
    trigger.setKey(TriggerKey.triggerKey("triggerkey:" + name))
    trigger.setName("triggerkey:" + name)

    job.setJobClass(jobClass)
    job.setJobDataMap(jobDataMap)

    logger.info(s"Set ${name} interval " + cron)
    trigger.setCronExpression(cron)

    scheduler.scheduleJob(job, trigger)
  }

  def start: Unit = {
    if (null == scheduler) {
      throw new ExceptionInInitializerError("initialize EsScheduler first")
    }
    scheduler.start()
  }

  def stop: Unit = {
    if (null != scheduler) {
      scheduler.shutdown(false)
    }
  }

  def clear: Unit = {
    if (null != scheduler) {
      try {
        scheduler.clear()
      } catch {
        case _: SchedulerException => //stop后clear会报错,忽略
      }
    }
  }
}
