package com.example.mypipsample

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.example.mypipsample.databinding.ActivityPipBinding
import com.example.mypipsample.databinding.LayoutPipBinding
import java.beans.PropertyChangeListener

/**
 * PIP 모드를 제공하는 [Activity][android.app.Activity]
 *
 * ---
 * __메모:__ PIP 화면은 최대 1:2.39 또는 2.39:1의 비율까지만 허용
 */
class PipActivity : AppCompatActivity() {
    private lateinit var fullScreenBinding: ActivityPipBinding
    private val pipBinding: LayoutPipBinding by lazy {
        LayoutPipBinding.inflate(layoutInflater)
    }
    private lateinit var pictureInPictureParamsBuilder: PictureInPictureParams.Builder
    private var isInPictureInPictureMode: Boolean? = null
    private val pipRemoteActionsHelper = PipRemoteActionsHelper(this)
    private val pipBroadcastReceiver = PipBroadcastReceiver()
    private val timeChangeListener = PropertyChangeListener { event ->
        when (event.propertyName) {
            StopwatchManager.PROPERTY_TIMESTAMP -> {
                if (isFinishing ||
                    isDestroyed) {
                    return@PropertyChangeListener
                }

                runOnUiThread {
                    if (isInPictureInPictureMode == true) {
                        pipBinding.timerTextView.text = event.newValue.toString()
                    } else {
                        fullScreenBinding.timerTextView.text = event.newValue.toString()
                    }
                }
            }
        }
    }
    private val modeChangeListener = PropertyChangeListener { event ->
        Log.d(TAG, "id : ${event.source}, " +
                "prev value : ${event.oldValue}, " +
                "current value : ${event.newValue}")

        when (event.propertyName) {
            PipBroadcastReceiver.PROPERTY_OUTPUT_MODE -> {
                if (isFinishing ||
                    isDestroyed ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    return@PropertyChangeListener
                }

                updatePictureInPictureActions(
                    pipRemoteActionsHelper.getRemoteActions(event.newValue as OutputMode)
                )
            }
        }
    }
    private val stopwatchManager = StopwatchManager().run {
        Log.d(TAG, "stopwatchManager is created")

        start(toTimestamp = true)
        this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fullScreenBinding = ActivityPipBinding.inflate(layoutInflater)

        setContentView(fullScreenBinding.root)
        stopwatchManager.changes
            .addPropertyChangeListener(timeChangeListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()

            updatePictureInPictureActions(
                pipRemoteActionsHelper.getRemoteActions(OutputMode.SMARTPHONE)
            )
            pipBinding.root
                .addOnLayoutChangeListener {
                        _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    if (left == oldLeft &&
                        top == oldTop &&
                        right == oldRight &&
                        bottom == oldBottom) {
                        return@addOnLayoutChangeListener
                    }

                    val rect = Rect()

                    pipBinding.root.getGlobalVisibleRect(rect)

                    Log.d(TAG, "View.OnLayoutChangeListener.onLayoutChange" +
                            ", rect left : ${rect.left}" +
                            ", top : ${rect.top}" +
                            ", right : ${rect.right}" +
                            ", bottom : ${rect.bottom}")
                }
        }

        prepareFullScreen()
    }

    /**
     * [Activity][android.app.Activity]의 launchMode가 singleTask인 상태에서
     * [startActivity(intent: Intent)][android.app.Activity.startActivity]를 호출했을 떄
     * [onCreate(savedInstanceState: Bundle?)][android.app.Activity.onCreate]대신 불리는 함수
     *
     * ---
     * __메모:__ 해당 함수에서는 PIP로부터 종료 [Action][android.content.Intent.getAction]을 받고
     * Activity를 종료할 때 History에 남지 않도록 하고, 이전의 Activity를 불러옴
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == ACTION_CLOSE) {
            finishAndRemoveTask()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                moveToPrevActivity()
            }
        }
    }

    /**
     * [Activity][android.app.Activity]가 백그라운드로 넘어갈 때 시스템에 의해 호출되는 함수
     *
     * ---
     * __메모:__ 해당 함수에서는 백그라운드로 넘어갈 때 PIP 모드로 전환될 수 있도록 구현함
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (isEnablePictureInPictureMode()) {
            pipBinding
            convertToPictureInPictureMode()
        }
    }

    /**
     * PIP 모드로 전환되거나 PIP 모드에서 나갈 때 시스템에 의해 호출되는 함수
     *
     * ---
     * __메모:__ 해당 함수에서는 PIP 모드 여부에 따라 다른 UI를 적용하도록 구현함
     *
     * ---
     * PIP 모드에서 기본적으로 제공되는 닫기 버튼 클릭시 Activity가 Histroy에 남지 않도록 하고, 이전의 Activity를 불러옴
     *
     * ---
     * 그 외 PIP 모드 전환되고 나갈 때는 PIP 모드 여부에 따라 View를 새로 그림
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        Log.d(TAG, "onPictureInPictureModeChanged" +
                "\nisInPictureInPictureMode : $isInPictureInPictureMode")

        when (lifecycle.currentState) {
            Lifecycle.State.CREATED -> {
                this@PipActivity.isInPictureInPictureMode = null

                finishAndRemoveTask()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    moveToPrevActivity()
                }
            }
            Lifecycle.State.STARTED -> {
                this@PipActivity.isInPictureInPictureMode = isInPictureInPictureMode

                if (isInPictureInPictureMode) {
                    supportActionBar?.hide()
                    setContentView(pipBinding.root)
                    registerReceiver(
                        pipBroadcastReceiver.apply {
                            changes.addPropertyChangeListener(modeChangeListener)
                        },
                        IntentFilter(PipBroadcastReceiver.ACTION_CHANGE_OUTPUT_MODE)
                    )
                } else {
                    supportActionBar?.show()
                    setContentView(fullScreenBinding.root)
                    prepareFullScreen()
                    unregisterReceiver(
                        pipBroadcastReceiver.apply {
                            changes.removePropertyChangeListener(modeChangeListener)
                        }
                    )
                }
            }
            else -> {
            }
        }

        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        stopwatchManager.stop()
        stopwatchManager.changes
            .removePropertyChangeListener(timeChangeListener)
        pipBroadcastReceiver.changes
            .removePropertyChangeListener(modeChangeListener)
        super.onDestroy()
    }

    override fun onBackPressed() {
    }

    /**
     * PIP 모드를 사용할 수 있는지 확인하는 함수f
     *
     * ---
     * __메모:__ 사용자가 단말기의 '설정' 메뉴를 통해 PIP를 허용하지 않을 수도 있음
     */
    private fun isEnablePictureInPictureMode() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /**
     * PIP 모드가 아닌 전체 화면을 설정하는 함수
     */
    private fun prepareFullScreen() {
        with(fullScreenBinding.convertToPipModeImageView) {
            if (isEnablePictureInPictureMode()) {
                isVisible = true

                setOnClickListener { convertToPictureInPictureMode() }
            } else {
                isVisible = false
            }
        }
    }

    /**
     * PIP의 [Action][android.app.RemoteAction]들을 재설정하는 함수
     *
     * ---
     * __메모:__ [Oreo][Build.VERSION_CODES.O] 이상의 버전에서만 해당 함수를 사용할 수 있음
     *
     * ---
     * 새로 설정하는 것이기 때문에 기존의 Action들이 새 Action들로 교체됨
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureActions(actions: ArrayList<RemoteAction>) {
        setPictureInPictureParams(
            pictureInPictureParamsBuilder.setActions(actions)
                .build()
        )
    }

    /**
     * PIP 모드로 전환하는 함수
     *
     * ---
     * __메모:__ [Oreo][Build.VERSION_CODES.O] 이상의 버전에서만 해당 함수를 사용할 수 있음
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertToPictureInPictureMode() {
        enterPictureInPictureMode(
            pictureInPictureParamsBuilder.setAspectRatio(
                Rational(HORIZONTAL_RATIO_QUANTITY, VERTICAL_RATIO_QUANTITY)
            ).build()
        )
        supportActionBar?.hide()
    }

    /**
     * PIP Activity 이전의 Activity를 Background에서(Background에 있다면) Foreground로 불러오는 함수
     *
     * ---
     * __메모:__ 로직에서 요구하는 버전은 [Marshmallow][Build.VERSION_CODES.M] 이상이지만,
     * PIP는 [Oreo][Build.VERSION_CODES.O] 이상의 버전에서 실행되므로 [Oreo][Build.VERSION_CODES.O]버전을
     * 요구하도록 적용
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveToPrevActivity() {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).appTasks
            .forEach { task ->
                Log.d(TAG, "task : ${task.taskInfo.topActivity?.className}")

                if (task.taskInfo
                        .baseIntent
                        .categories
                        ?.contains(Intent.CATEGORY_LAUNCHER) == true) {
                    task.moveToFront()

                    return
                }
            }
    }

    companion object {
        private val TAG = PipActivity::class.java.simpleName
        private const val HORIZONTAL_RATIO_QUANTITY = 2
        private const val VERTICAL_RATIO_QUANTITY = 1
        const val ACTION_CLOSE = "ACTION_CLOSE"
    }
}
