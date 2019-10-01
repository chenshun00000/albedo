package com.albedo.java.modules.quartz.config;

import com.albedo.java.common.core.exception.TaskException;
import com.albedo.java.common.core.util.Json;
import com.albedo.java.common.core.vo.ScheduleVo;
import com.albedo.java.modules.quartz.domain.Job;
import com.albedo.java.modules.quartz.util.ScheduleUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.util.Assert;

@Slf4j
@AllArgsConstructor
public class ScheduleReceiver {

	private final Scheduler scheduler;
	/**
	 *  收到通道的消息之后执行的方法
	 * @param message
	 */
	public void receiveMessage(String message) throws TaskException, SchedulerException {
		log.info("receiveMessage===>" + message);
		ScheduleVo scheduleVo = Json.parseObject(message, ScheduleVo.class);
		Assert.isTrue(scheduleVo!=null, "scheduleVo cannot be null");
		Assert.isTrue(scheduleVo.getMessageType()!=null, "scheduleVo cannot be null");
		String jobId = scheduleVo.getJobId();
		String jobGroup = scheduleVo.getJobGroup();
		switch (scheduleVo.getMessageType()){
			case ADD:
				ScheduleUtils.createScheduleJob(scheduler, Json.parseObject(scheduleVo.getData(), Job.class));
				break;
			case UPDATE:
				updateSchedulerJob(Json.parseObject(scheduleVo.getData(), Job.class), jobGroup);
				break;
			case PAUSE:
				scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
				break;
			case RESUME:
				scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
				break;
			case DELETE:
				scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
				break;
			case RUN:
				scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup));
				break;
			default:
				log.warn("unkown message type :" + message);
				break;
		}


	}

	/**
	 * 更新任务
	 *
	 * @param job      任务对象
	 * @param jobGroup 任务组名
	 */
	public void updateSchedulerJob(Job job, String jobGroup) throws SchedulerException, TaskException {
		String jobId = job.getId();
		// 判断是否存在
		JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
		if (scheduler.checkExists(jobKey)) {
			// 防止创建时存在数据问题 先移除，然后在执行创建操作
			scheduler.deleteJob(jobKey);
		}
		ScheduleUtils.createScheduleJob(scheduler, job);
	}


}
