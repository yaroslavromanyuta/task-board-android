package com.rounds.test.to_dolist.qa

import android.app.Activity
import android.content.Intent

/**
 * The release half of the QA hook: nothing.
 *
 * `MainActivity` calls [install] unconditionally, and the source-set split — rather than a
 * `BuildConfig.DEBUG` branch — is what guarantees the debug implementation is physically absent from
 * a release build rather than merely unreachable in it.
 */
@Suppress("UNUSED_PARAMETER")
object QaMockControls {
    fun install(activity: Activity, intent: Intent?) = Unit
}
