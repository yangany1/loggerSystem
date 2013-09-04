package com.zhidaoba.loggersystem.core;

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
			new RelevancyThread().run();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new LogMainSystem().run();
	}
}
