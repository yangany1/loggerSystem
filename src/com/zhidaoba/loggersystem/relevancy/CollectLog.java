package com.zhidaoba.loggersystem.relevancy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;

import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.common.Constants;
import com.zhidaoba.loggersystem.common.DatabaseHandler;

/**
 * 读取log文件，并将其按照需要的格式写入mongodb数据库
 * 
 * @author luo
 * 
 */
public class CollectLog {

	private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static SimpleDateFormat sDateFormat = new SimpleDateFormat(
			TIME_FORMAT);

	// change list是分析日志数据得到的将要写到日志数据库的对象列表
	private static ArrayBlockingQueue<RelevancyObject> changeList;

	/**
	 * 日志文件的处理
	 */
	public static void readLog() {
		if (changeList == null) {
			changeList = new ArrayBlockingQueue<RelevancyObject>(
					ConfigHandler.getLogQueueSize());
		}
		changeList.clear();
		getLogFromLogFile();
		writeLogtoMongo();
	}

	/**
	 * 读入日志文件，按格式存入changeList
	 */
	public static void getLogFromLogFile() {
		long logtime = 0;
		String action = "";
		String content = "";
		String uid = "";
		String tmp = "";
		String dialog_id="";

		File dir = new File(ConfigHandler.getLogDir());
		long lastReadTime = ConfigHandler.getRevLogLastReadTime();
		int lastReadLineNumber = ConfigHandler.getRevLogLastReadLineNumber();
		String lastReadFileName = ConfigHandler.getRevLogLastReadFileName();

		if (dir.isDirectory()) {
			File[] filelist = dir.listFiles();
			Arrays.sort(filelist, new Comparator<File>() {
				public int compare(File f1, File f2) {
					return Long.valueOf(f1.lastModified()).compareTo(
							f2.lastModified());
				}
			});

			for (File file : filelist) {
				if (ConfigHandler.getRevLogLastReadTime() < file.lastModified()
						&& !file.getAbsolutePath().contains(
								ConfigHandler.getLogFile())) {
					System.out.println("read file:" + file.getName());
					BufferedReader br = null;
					try {
						br = new BufferedReader(new FileReader(file));
						String line;
						int status = 0;
						int i = 0;
						boolean isSameFile = false;
						if (file.getName().equals(lastReadFileName)) {
							isSameFile = true;
						}
						while ((line = br.readLine()) != null) {
							i++;
							if (isSameFile && i <= lastReadLineNumber) {
								continue;
							}
							line = line.trim();
							if (status == 0
									&& line.startsWith(Constants.LOG_BEGIN)) {
								logtime = sDateFormat.parse(
										line.split(Constants.LOG_BEGIN)[1])
										.getTime();
								status = 1;
								System.out.println("logtime:" + logtime);
							} else if (line.startsWith(Constants.LOG_ACTION)) {
								tmp = line
										.split(Constants.LOG_ACTION_SPLIT_LEFT)[1];
								action = tmp
										.substring(
												0,
												tmp.length()
														- Constants.LOG_ACTION_SPLIT_RIGHT
																.length());
								System.out.println("action:" + action);
							} else if (line.startsWith(Constants.LOG_CONTENT)) {
								content = line.split(Constants.LOG_CONTENT)[1];
								System.out.println("content:" + content);
							} else if (line.startsWith(Constants.LOG_UID)) {
								uid = line.split(Constants.LOG_UID)[1];
								System.out.println("userid:" + uid);
							} else if (line.startsWith(Constants.LOG_DIALOG_ID)) {
								dialog_id = line.split(Constants.LOG_DIALOG_ID)[1];
								System.out.println("dialog:" + uid);}
							else if (status == 1
									&& line.startsWith(Constants.LOG_END)) {
								status = 0;

								if (logtime >= lastReadTime && !uid.equals("")) {
									System.out.println("add to list");
									changeList.add(new RelevancyObject(uid,
											logtime, action, content,dialog_id));
									lastReadTime = logtime;
									lastReadLineNumber = i;
									lastReadFileName = file.getName();
								}
								if (changeList.size() >= ConfigHandler
										.getLogQueueSize()) {
									ConfigHandler.getLogger().warning(
											"Too Many Logs.");
									break;
								}
								logtime = 0;
								action = "";
								content = "";
								uid = "";
								dialog_id="";
							}

						}
						System.out.println("list size=" + changeList.size());
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (br != null) {
								br.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
			}

		} else {
			ConfigHandler.getLogger().warning("LOG Path Error!");
			System.exit(-1);
		}

		ConfigHandler.setRevLogLastReadTime(lastReadTime);
		ConfigHandler.setRevLogLastReadFileName(lastReadFileName);
		ConfigHandler.setRevLogLastReadLineNumber(lastReadLineNumber);
		System.out.println("save property");
		System.gc();
		ConfigHandler.save();
	}

	public static void writeLogtoMongo() {
		DatabaseHandler.writeLogToMongoDB(changeList);
	}

	public static void main(String[] args) {
		ConfigHandler.load();
		CollectLog.readLog();
	}
}
