package com.Conversant.parser;

public class RTBRequest {

	private String type;
	private long Time;
	private double Value;
	private String dataCenter;
	
	
	public RTBRequest(String type, long time, double value, String dataCenter) {
		this.type = type;
		this.Time = time;
		this.Value = value;
		this.dataCenter = dataCenter;
	}

	public String getType() {
		return type;
	}

	public long getTime() {
		return Time;
	}

	public double getValue() {
		return Value;
	}

	public String getDataCenter() {
		return dataCenter;
	}
}
	
	
