package com.rounds.test.to_dolist.core.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
// Priority colours sit outside the Material scheme on purpose: they must stay stable under dynamic
// colour, otherwise "high" could end up looking calmer than "low" on some wallpapers.
//
// Two sets, because one cannot serve both schemes: these are chosen for contrast against a light
// surface, and the same three on a dark one are muddy and much closer to each other than the ordering
// they encode. The dark set keeps the same hues and lifts the lightness (FR-14).
val PriorityLow = Color(0xFF3F8F4E)
val PriorityMedium = Color(0xFFB8860B)
val PriorityHigh = Color(0xFFC1392B)

val PriorityLowDark = Color(0xFF7BD695)
val PriorityMediumDark = Color(0xFFF0C24B)
val PriorityHighDark = Color(0xFFFF8A80)
