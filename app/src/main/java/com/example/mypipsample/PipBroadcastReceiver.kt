package com.example.mypipsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.beans.PropertyChangeSupport
import java.util.EnumSet

class PipBroadcastReceiver : BroadcastReceiver() {
    private var currentOutputMode = OutputMode.SMARTPHONE
    val changes = PropertyChangeSupport(this)
    private val outputModes = EnumSet.allOf(OutputMode::class.java).toList()

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "action : ${intent?.action}")

        when (intent?.action) {
            ACTION_CHANGE_OUTPUT_MODE -> {
                changes.firePropertyChange(
                    PROPERTY_OUTPUT_MODE,
                    currentOutputMode,
                    nextOutputMode()
                )
            }
        }
    }

    private fun nextOutputMode(): OutputMode {
        currentOutputMode = outputModes[
                (outputModes.indexOf(currentOutputMode) + 1) % OutputMode.values().size
        ]

        return currentOutputMode
    }

    companion object {
        const val PROPERTY_OUTPUT_MODE = "PROPERTY_OUTPUT_MODE"
        const val ACTION_CHANGE_OUTPUT_MODE = "ACTION_CHANGE_OUTPUT_MODE"
        private val TAG = PipBroadcastReceiver::class.java.simpleName
    }
}
