package com.zhidaoba.loggersystem.core;

import com.zhidaoba.emergency.EmergencyConfigHandler;
import com.zhidaoba.emergency.EmergencySortThread;
import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.relevancy.RelevancyThread;
/**
 * log系统的启动线程
 * @author luo
 *
 */
public class LogMainSystem {

	private void init(){
		ConfigHandler.load();
	}
	
	private void run(){
		try{
			this.init();
			//更新标签相似度
			new RelevancyThread().run();
			//紧急库排序
			EmergencyConfigHandler.load();
			new EmergencySortThread().updateRank();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new LogMainSystem().run();
	}
}
