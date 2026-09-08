package com.rounds.test.to_dolist.core.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rounds.test.to_dolist.core.ui.R
import com.rounds.test.to_dolist.tasks.error.DataError

/**
 * The one place a typed domain failure becomes user-facing text. Keeping it here (and not in the
 * domain) is why `:lib:tasks-api` needs no resources and no locale.
 */
@Composable
fun DataError.asMessage(): String = stringResource(
    when (this) {
        DataError.Network -> R.string.core_ui_error_network
        DataError.Timeout -> R.string.core_ui_error_timeout
        DataError.NotFound -> R.string.core_ui_error_not_found
        DataError.Conflict -> R.string.core_ui_error_conflict
        DataError.Unknown -> R.string.core_ui_error_unknown
    },
)
