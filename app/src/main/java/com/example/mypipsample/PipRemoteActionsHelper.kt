package com.example.mypipsample

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi

class PipRemoteActionsHelper(private val context: Context) {
    fun getRemoteActions(outputMode: OutputMode): ArrayList<RemoteAction> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayListOf(
                createRemoteAction(
                    resId = when (outputMode) {
                        OutputMode.SMARTPHONE -> R.drawable.ic_smartphone
                        OutputMode.SPEAKERPHONE -> R.drawable.ic_speaker_phone
                        OutputMode.BLUETOOTH -> R.drawable.ic_bluetooth
                    },
                    action = PipBroadcastReceiver.ACTION_CHANGE_OUTPUT_MODE,
                    isActivityAction = false,
                    extra = OUTPUT_MODE to outputMode
                ),
                createRemoteAction(
                    resId = R.drawable.ic_call_end,
                    action = PipActivity.ACTION_CLOSE,
                    isActivityAction = true
                )
            )
        } else {
            arrayListOf()
        }

    /**
     * PIP에 필요한 [Action][android.app.RemoteAction]을 생성하는 함수
     *
     * ---
     * __메모:__ [Oreo][Build.VERSION_CODES.O] 이상의 버전에서만 해당 함수를 사용할 수 있음
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes resId: Int,
        action: String,
        isActivityAction: Boolean,
        extra: Pair<String, OutputMode> = OUTPUT_MODE to OutputMode.SMARTPHONE
    ): RemoteAction =
        RemoteAction(
            Icon.createWithResource(context, resId),
            REMOTE_ACTION_TITLE,
            REMOTE_ACTION_CONTENT_DESCRIPTION,
            if (isActivityAction) {
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, PipActivity::class.java).setAction(action),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(action).putExtra(extra.first, extra.second),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )
            }
        )

    companion object {
        private const val REMOTE_ACTION_TITLE = "REMOTE_ACTION_TITLE"
        private const val REMOTE_ACTION_CONTENT_DESCRIPTION = "REMOTE_ACTION_CONTENT_DESCRIPTION"
        private const val OUTPUT_MODE = "OUTPUT_MODE"
    }
}
