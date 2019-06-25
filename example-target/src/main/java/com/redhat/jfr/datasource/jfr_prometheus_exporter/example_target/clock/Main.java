package com.redhat.jfr.datasource.jfr_prometheus_exporter.example_target.clock;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

public class Main {

	@Label("Clock Event")
	@Description("+1s as the time goes")
	@Category("Clock")
	public static class ClockEvent extends Event {
		@Label("Time")
		@Description("Time in millis.")
		private long timeMillis;

		public ClockEvent() {
			timeMillis = System.currentTimeMillis();
		}

		public void setTimeMillis(long timeMillis) {
			this.timeMillis = timeMillis;
		}

		public long getTimeMillis() {
			return timeMillis;
		}

		@Override
		public String toString() {
			return timeMillis + "ms, " + (timeMillis / 1000 % 2 == 0 ? "tik" : "tok");
		}
	}

	public static void main(String[] args) throws InterruptedException {
		while (true) {
			ClockEvent e = new ClockEvent();
			System.out.println(e);

			e.begin();
			Thread.sleep(1000);
			System.gc();
			e.end();
			e.commit();
		}
	}
}
