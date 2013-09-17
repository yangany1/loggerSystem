package com.zhidaoba.emergency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.zhidaoba.loggersystem.common.*;

public class EmergencySortThread {
	private List<Emergency> emergencyList = new ArrayList<Emergency>();
	public EmergencySortThread() {
	}
	public void updateRank() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				init();
				Collections.sort(emergencyList);
				output();
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 0, Constants.REPEATIME);

	}

	public void output() {
		for (Emergency e : emergencyList) {
			System.out.println(e.toString());
		}
		System.out.println("------------------");
		System.out.println("------------------");
		System.out.println("------------------");
	}

	public void init() {
		EmergencyDatabaseHandler.getEmergencyFromMongo(emergencyList);
		try {
			for (int i = 0; i < emergencyList.size(); i++) {
				Emergency e = emergencyList.get(i);
				int returnFlag = e.calculateRankScore();
				// 检查返回值,如果计算rank失败，则设置排序值为0
				if (returnFlag != Constants.SUCCESS) {
					e.setOrder_value(0);
				}
				boolean success = EmergencyDatabaseHandler.updateEmergency(e);
				if (success) {
					EmergencyConfigHandler.getLogger().warning(
							"question_id=" + e.getQuestion_id() + "更新数据库成功！");
				} else {
					EmergencyConfigHandler.getLogger().warning(
							"question_id=" + e.getQuestion_id() + "更新数据库失败！");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	/**
	 * 紧急队列取出问题到客户端
	 * 
	 * @param num
	 *            取出问题的个数
	 * @return 紧急问题列表
	 */
	public List<Emergency> getSomeEmergency(int num) {
		List<Emergency> elist = new ArrayList<Emergency>();
		// 每次选择最紧急的问题存入elist
		for (int i = 0; i < num; i++) {
			if (emergencyList.size() > 0)
				elist.add(emergencyList.get(0));
			else
				break;
			emergencyList.remove(0);
		}
		// 将这些问题访问队尾
		for (Emergency e : elist) {
			emergencyList.add(e);
		}
		return elist;

	}
	public static void main(String[] args) {
		EmergencySortThread em = new EmergencySortThread();
		EmergencyConfigHandler.load();
		em.updateRank();
	}
}
