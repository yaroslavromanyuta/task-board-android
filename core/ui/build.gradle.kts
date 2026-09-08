plugins {
    id("todo.android.library.compose")
}

android {
    namespace = "com.rounds.test.to_dolist.core.ui"
}

dependencies {
    // The design system renders domain types (priority, typed errors), so it sees the contracts.
    api(project(":lib:tasks-api"))
}
