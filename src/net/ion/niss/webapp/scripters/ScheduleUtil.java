package net.ion.niss.webapp.scripters;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

public class ScheduleUtil {

	private Date day;
	public ScheduleUtil(){
		this.day = new Date() ;
	}
	
	public int nextDate() {
		return nextDate(1) ;
	}
	
	public int nextDate(int amount) {
		return DateUtils.addDays(this.day, amount).getDate() ;
	}
	
}
