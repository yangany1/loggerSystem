package com.zhidaoba.emergency;

import java.io.Serializable;
import java.util.Date;

import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.common.Constants;


public class Emergency implements Comparable<Emergency>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
//	@SerializedName("question_id")
	private String question_id;
//	@SerializedName("created_at")
	private Date created_at;
//	@SerializedName("updated_at")
	private Date updated_at;

//	@SerializedName("showed_number")
	private int showed_number;
//	@SerializedName("mean_matching_degree")
	private double mean_matching_degree;
//	@SerializedName("order_value")
	private double order_value;

	public String getQuestion_id() {
		return question_id;
	}

	public void setQuestion_id(String question_id) {
		this.question_id = question_id;
	}

	public Date getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Date created_at) {
		this.created_at = created_at;
	}

	public Date getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(Date updated_at) {
		this.updated_at = updated_at;
	}

	public int getShowed_number() {
		return showed_number;
	}

	public void setShowed_number(int showed_number) {
		this.showed_number = showed_number;
	}

	public double getMean_matching_degree() {
		return mean_matching_degree;
	}

	public void setMean_matching_degree(double mean_matching_degree) {
		this.mean_matching_degree = mean_matching_degree;
	}

	public double getOrder_value() {
		return order_value;
	}

	public void setOrder_value(double order_value) {
		this.order_value = order_value;
	}

	/**
	 * 计算rank分数
	 */
	public int calculateRankScore() {


		long intergEmergTime = (System.currentTimeMillis() - created_at
				.getTime()) / Constants.MILLSECONDSTOMINUTE;
//		System.out.println("intertime=" + intergEmergTime);
		if (intergEmergTime > Constants.OVERTIME_MINUTE) {
			 ConfigHandler.getLogger().warning("question_id="+getQuestion_id()+"超时！");
			return Constants.OVERTIME;
		}

		int emergPlanShow = (int) (20 + intergEmergTime * 4);
		if (emergPlanShow <= showed_number) {
			ConfigHandler.getLogger().warning("question_id="+getQuestion_id()+"显示次数超过最大次数！");
			return Constants.OVERSHOWED;
		}
		this.order_value = (emergPlanShow - showed_number)
				* mean_matching_degree;
		return Constants.SUCCESS;
	}

	@Override
	public int compareTo(Emergency e) {
		// TODO Auto-generated method stub
		double rank = this.order_value - e.getOrder_value();
		if (rank > 0)
			return -1;
		else if (rank == 0)
			return 0;
		else
			return 1;

	}



	@Override
	public String toString() {
		return "Emergency [question_id=" + question_id + ", created_at="
				+ created_at + ", updated_at=" + updated_at
				+ ", showed_number=" + showed_number
				+ ", mean_matching_degree=" + mean_matching_degree
				+ ", order_value=" + order_value + "]";
	}

	public Emergency(String question_id, Date created_at, Date updated_at,
			int showed_number, double mean_matching_degree, double order_value) {
		super();
		this.question_id = question_id;
		this.created_at = created_at;
		this.updated_at = updated_at;
		this.showed_number = showed_number;
		this.mean_matching_degree = mean_matching_degree;
		this.order_value = order_value;
	}

	public Emergency() {
		super();
		// TODO Auto-generated constructor stub
	}

}
