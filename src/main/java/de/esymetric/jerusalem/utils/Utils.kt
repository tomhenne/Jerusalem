package de.esymetric.jerusalem.utils

object Utils {
    fun memInfoStr(): String {
        return ((Runtime.getRuntime().freeMemory() / 1024L / 1024L)
            .toString() + " MB / "
                + Runtime.getRuntime().totalMemory() / 1024L / 1024L
                + " MB ")
    }

    private fun d02(d: Int) = if (d < 10) "0$d" else d.toString()

    fun formatTimeStopWatch(ticks: Long): String {
        var sec = (ticks / 1000L).toInt()
        val days = sec / 86400
        sec -= days * 86400
        val hours = sec / 3600
        sec -= hours * 3600
        val min = sec / 60
        sec -= min * 60
        return ("" + (if (days > 0) "$days." else "") + hours + ":"
                + d02(Math.abs(min)) + ":" + d02(Math.abs(sec)))
    }
}