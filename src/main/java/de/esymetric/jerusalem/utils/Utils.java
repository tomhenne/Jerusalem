package de.esymetric.jerusalem.utils;

public class Utils {
	public static String memInfoStr() {
		return (Runtime.getRuntime().freeMemory() / 1024L / 1024L)
		+ " MB / "
		+ (Runtime.getRuntime().totalMemory() / 1024L / 1024L)
		+ " MB ";
	}

	static String D02(int d) {
		return d < 10 ? "0" + d : String.valueOf(d);
	}

	public static String formatTimeStopWatch(long ticks) {
		int sec = (int) (ticks / 1000L);

		int days = sec / 86400;
		sec -= days * 86400;
		int hours = sec / 3600;
		sec -= hours * 3600;
		int min = sec / 60;
		sec -= min * 60;

		return "" + (days > 0 ? (days + ".") : "") + hours + ":"
				+ D02(Math.abs(min)) + ":" + D02(Math.abs(sec));
	}

}
