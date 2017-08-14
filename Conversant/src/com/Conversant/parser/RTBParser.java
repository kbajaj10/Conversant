package com.Conversant.parser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class RTBParser {
	
	private final char DEFAULT_SEPARATOR = ',';
	private static List<RTBRequest> requestList = new ArrayList<RTBRequest>();
	private static HashMap<Long, Double> dataMapI = new HashMap<Long, Double>();
	private static HashMap<Long, Double> dataMapS = new HashMap<Long, Double>();
	private static HashMap<Long, Double> dataMapA = new HashMap<Long, Double>();
	private static List<Long> datesI = new ArrayList<Long>();
	private static List<Long> datesS = new ArrayList<Long>();
	private static List<Long> datesA = new ArrayList<Long>();
	private static List<Double> valuesI = new ArrayList<Double>();
	private static List<Double> valuesS = new ArrayList<Double>();
	private static List<Double> valuesA = new ArrayList<Double>();
			
	public static void main(String[] args) throws IOException {
		RTBParser rtb = new RTBParser();
		int minutes = 0;
		File rtbData = new File("data.Montoya.txt");
		rtb.parseData(rtbData);
		rtb.filterByDataCenter();
		
		Scanner input = new Scanner(System.in);
		System.out.print("Please enter time interval to analyze data over or 0 to exit: ");
		minutes = input.nextInt();
		if (minutes > 0) {		
			rtb.analyzeTimeBlocks(RTBParser.datesI, minutes, RTBParser.dataMapI, 'I');
			rtb.analyzeTimeBlocks(RTBParser.datesS, minutes, RTBParser.dataMapS, 'S');
			rtb.analyzeTimeBlocks(RTBParser.datesA, minutes, RTBParser.dataMapA, 'A');
		}
		else {
			System.out.println("Invalid time period entered or Exit called, application exiting");
			System.exit(0);		
		}
		input.close();		
	}
	
	public void parseData(File dataFile) { //parses data file creating rtb objects
		try {
			Scanner fileScan = new Scanner(dataFile);
			String[] values;
			String type;
			long time;
			double value;
			String dataCenter;
			
			while (fileScan.hasNextLine()) {
				String line = fileScan.nextLine();
				values = line.split("\\s+");
				type = values[0];
				time = Long.parseLong(values[1]);
				value = Double.parseDouble(values[2]);
				dataCenter = values[3].substring(3);
				requestList.add(new RTBRequest(type, time, value, dataCenter));
			}
			fileScan.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
		
	public void filterByDataCenter() { //seperates the data by data center
						
		for (RTBRequest r : requestList) {
			if (r.getDataCenter().equals("I")) {
				 RTBParser.datesI.add(r.getTime());
				 RTBParser.valuesI.add(r.getValue());
				 RTBParser.dataMapI.put(r.getTime(), r.getValue());
			}
			if (r.getDataCenter().equals("S")) {
				RTBParser.datesS.add(r.getTime());
				RTBParser.valuesS.add(r.getValue());
				RTBParser.dataMapS.put(r.getTime(), r.getValue());
			}
			if (r.getDataCenter().equals("A")) {
				RTBParser.datesA.add(r.getTime());
				RTBParser.valuesA.add(r.getValue());
				RTBParser.dataMapA.put(r.getTime(), r.getValue());
			}
		}
	}
		
	public void analyzeTimeBlocks(List<Long> dateList, int minutes, HashMap<Long, Double> dataCenterMap, char dataCenter) throws IOException { //chunks data into specified interval and writes data averages in chronological order
		double averageValue;
		int localInnerCount = 0, totalInnerCount = 0, i = 0; 
		boolean keepAlive = true;
		TreeMap<Double,Long> averageTimeMap = new TreeMap<Double,Long>();
		Duration gap = Duration.ofMillis(minutes*60*1000);
		Instant startInstant =  Instant.ofEpochSecond(dateList.get(0));
		Instant endInstant =  startInstant.plus(gap);
		Instant saveStartInstant;
		List<Double> averageValues = new ArrayList<Double>();
	
		StringBuilder sb = new StringBuilder();
		sb.append("rtb.request averages over time period " +minutes+ " minutes for Datacenter "+dataCenter+ " ordered by time interval")
				.append("\n")
				.append("Data Center" + DEFAULT_SEPARATOR)
				.append("Start time" + DEFAULT_SEPARATOR)
				.append("End time" + DEFAULT_SEPARATOR)
				.append("RTB.Request Value Average" + DEFAULT_SEPARATOR)
				.append("\n");
				
		while (keepAlive) {
			i = 0 + totalInnerCount;
			double sum = 0;
			localInnerCount = 0;
			saveStartInstant = startInstant;
			
			sb.append(dataCenter)
					.append(DEFAULT_SEPARATOR)
					.append(Date.from(startInstant))
					.append(DEFAULT_SEPARATOR)
					.append(Date.from(endInstant))
					.append(DEFAULT_SEPARATOR);
			while (Date.from(startInstant).compareTo(Date.from(endInstant)) < 0 && keepAlive) {
				if (i < dateList.size()) {
					startInstant =  Instant.ofEpochSecond(dateList.get(i));
					sum += dataCenterMap.get(dateList.get(i));
					localInnerCount++;
					i++;
					totalInnerCount++;
				}
				else {
					keepAlive = false;
				}
			}
			if (localInnerCount != 0) averageValue = sum / localInnerCount;
			else averageValue = 0;
			averageValues.add(averageValue);
			sb.append(averageValue).append("\n");
			averageTimeMap.put(averageValue,saveStartInstant.toEpochMilli()/1000);
			startInstant = endInstant;
			endInstant = startInstant.plus(gap);
		}
			sb.append("\n\n");
			sb.append(presentSortedAverages(dataCenter, averageTimeMap, minutes));
			String fileName = new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date());
			
			File outputFile = new File("Results-" + fileName);
			String filePath = outputFile.getAbsolutePath();
			
			if (!outputFile.exists()) {
	            outputFile.createNewFile();
	            System.out.println("File with results created at: " +filePath);
	            
	        }
			report(outputFile,sb);
	}
	
	public StringBuilder presentSortedAverages(char dataCenter, TreeMap<Double,Long> sortedMap, int duration) { //creates stringbuilder used for reporting for data sorted by average
		StringBuilder sb = new StringBuilder();
		Instant startInstant;
		Instant endInstant;
		Duration gap = Duration.ofMillis(duration*60*1000);
		double average;
		long time;
	
		sb.append("rtb.request averages averages sorted by average value over period " +duration+ " minutes for Datacenter "+dataCenter)
				.append("\n")
				.append("Data Center" + DEFAULT_SEPARATOR)
				.append("Start time" + DEFAULT_SEPARATOR)
				.append("End time" + DEFAULT_SEPARATOR)
				.append("RTB.Request Value Average" + DEFAULT_SEPARATOR)
				.append("\n");
		
		for(Map.Entry<Double,Long> entry : sortedMap.entrySet()) {
			  average = entry.getKey();
			  time = entry.getValue();
			  startInstant =  Instant.ofEpochSecond(time);
			  endInstant = startInstant.plus(gap);
			  
			  sb.append(dataCenter)
	        	.append(DEFAULT_SEPARATOR)
	        	.append(Date.from(startInstant))
	        	.append(DEFAULT_SEPARATOR)
	        	.append(Date.from(endInstant))
	        	.append(DEFAULT_SEPARATOR)
	        	.append(average)
			  	.append("\n");
		}
		sb.append("\n\n");
		return sb;
	}
		
	public void report(File file,StringBuilder sb) throws IOException {	//writes report
		
		FileWriter writer = new FileWriter(file.getAbsolutePath(), true);
        BufferedWriter buffWriter = new BufferedWriter(writer);
        
        buffWriter.write(sb.toString());
        buffWriter.close();
        
	}
}
