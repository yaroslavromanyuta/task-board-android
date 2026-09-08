package com.rounds.test.to_dolist.qa

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rounds.test.to_dolist.data.tasks.api.FakeTaskApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The QA suite's hand on the mock source's dice.
 *
 * `FakeTaskApi` fails ~15% of calls by design, which is the point of it — but it makes a scripted
 * journey unrunnable: a scenario that touches the source four times fails to reach its own
 * precondition about half the time. `failureRate` was already a `var` so a demo could zero it; this
 * is the part that lets a device reach it.
 *
 * Two knobs:
 *  - `failureRate`, so a scenario reaches its own precondition instead of losing a coin flip;
 *  - `latencyMillis`, because a 300-800 ms call finishes before a UI dump can be read, which makes
 *    the loading state (FR-07) and every write-in-flight race invisible to a scripted test.
 *
 * Two entry points, because two moments matter:
 *  - **intent extras** (`qa_failure_rate`, `qa_latency_ms`), applied from `MainActivity.onCreate`
 *    before the first composition, so they land ahead of the list's initial refresh;
 *  - the [QaControlReceiver] **broadcast**, for changing either on a running app — which is the only
 *    way to test a failure arriving over content that is already on screen.
 *
 * This file exists only in the `debug` source set. The `release` variant compiles a no-op object
 * with the same signature, so nothing here can ship. See `qa/README.md`.
 */
object QaMockControls {

    /** Applies whichever of the QA extras a launch intent carries. */
    fun install(activity: Activity, intent: Intent?) {
        apply(activity, intent?.read(EXTRA_FAILURE_RATE), intent?.read(EXTRA_LATENCY_MS))
    }

    internal fun apply(context: Context, rawRate: String?, rawLatency: String?) {
        if (rawRate == null && rawLatency == null) return
        val api = EntryPointAccessors
            .fromApplication(context.applicationContext, QaEntryPoint::class.java)
            .taskApi()

        if (rawRate != null) {
            val rate = rawRate.toDoubleOrNull()
            if (rate == null || rate !in 0.0..1.0) {
                Log.w(TAG, "Ignored failure rate \"$rawRate\" - expected a number in 0.0..1.0")
            } else {
                api.failureRate = rate
                Log.i(TAG, "failureRate = $rate")
            }
        }

        if (rawLatency != null) {
            val latency = rawLatency.toLongOrNull()
            if (latency == null || latency < 0) {
                Log.w(TAG, "Ignored latency \"$rawLatency\" - expected a non-negative whole number")
            } else {
                api.latencyMillis = latency..latency
                Log.i(TAG, "latencyMillis = $latency")
            }
        }
    }

    /** `--es`, `--ef` and `--ei` all reach here, so a run script cannot get the flag subtly wrong. */
    internal fun Intent.read(key: String): String? = when {
        !hasExtra(key) -> null
        else -> getStringExtra(key)
            ?: getFloatExtra(key, Float.NaN).takeUnless { it.isNaN() }?.toString()
            ?: getIntExtra(key, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE }?.toString()
    }

    internal const val TAG = "QaMockControls"
    internal const val EXTRA_FAILURE_RATE = "qa_failure_rate"
    internal const val EXTRA_LATENCY_MS = "qa_latency_ms"
}

/**
 * The Hilt singleton the controls reach through. Declared here rather than in `:data:tasks` so the
 * production graph does not grow a QA-shaped hole.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface QaEntryPoint {
    fun taskApi(): FakeTaskApi
}

/**
 * Changes either knob on a running app. The broadcast must name the component explicitly, because
 * Android 8 stopped delivering implicit broadcasts to manifest-declared receivers:
 *
 * ```
 * adb shell am broadcast -n com.rounds.test.to_dolist/.qa.QaControlReceiver -a com.rounds.test.to_dolist.QA_SET_FAILURE_RATE --es qa_failure_rate 1.0
 * ```
 */
class QaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = with(QaMockControls) {
        val rate = intent.read(EXTRA_FAILURE_RATE)
        val latency = intent.read(EXTRA_LATENCY_MS)
        if (rate == null && latency == null) {
            Log.w(TAG, "Broadcast carried neither \"$EXTRA_FAILURE_RATE\" nor \"$EXTRA_LATENCY_MS\"")
        }
        apply(context, rate, latency)
    }
}
