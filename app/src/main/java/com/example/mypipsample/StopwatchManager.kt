package com.example.mypipsample

import android.util.Log
import java.beans.PropertyChangeSupport
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class StopwatchManager {
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var seconds = -1L
    val changes = PropertyChangeSupport(this)

    fun start(toTimestamp: Boolean = false) {
        if (timer != null) {
            Log.d(TAG, "stopwatch started")

            return
        }

        Log.d(TAG, "stopwatch started")

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
//                Log.d(TAG, "current seconds : $seconds, next seconds : ${seconds + 1}")

                if (toTimestamp) {
                    changes.firePropertyChange(
                        PROPERTY_TIMESTAMP,
                        toTimestamp(seconds),
                        toTimestamp(++seconds)
                    )
                } else {
                    changes.firePropertyChange(PROPERTY_SECONDS, seconds, ++seconds)
                }
            }
        }

        timer?.schedule(timerTask, 0L, A_SECOND_TO_MILLIS)
    }

    fun stop() {
        timer?.cancel()
        timerTask?.cancel()

        timer = null
        timerTask = null
        seconds = -1L
    }

    private fun toTimestamp(seconds: Long): String {
        var remainSeconds = seconds
        val hours = remainSeconds / AN_HOUR_TO_SECONDS
        remainSeconds %= AN_HOUR_TO_SECONDS
        val minutes = remainSeconds / A_MINUTE_TO_SECONDS
        remainSeconds %= A_MINUTE_TO_SECONDS

        return StringBuilder()
            .append(if (hours > 0) String.format("%02d:", hours) else "")
            .append(String.format("%02d:", minutes))
            .append(String.format("%02d", remainSeconds))
            .toString()
    }

    companion object {
        const val PROPERTY_SECONDS = "PROPERTY_SECONDS"
        const val PROPERTY_TIMESTAMP = "PROPERTY_TIMESTAMP"
        private val AN_HOUR_TO_SECONDS = TimeUnit.HOURS.toSeconds(1)
        private val A_MINUTE_TO_SECONDS = TimeUnit.MINUTES.toSeconds(1)
        private val A_SECOND_TO_MILLIS = TimeUnit.SECONDS.toMillis(1)
        private val TAG = StopwatchManager::class.java.simpleName
    }
}
