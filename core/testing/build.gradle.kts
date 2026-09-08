plugins {
    id("todo.android.library")
}

android {
    namespace = "com.rounds.test.to_dolist.core.testing"
}

dependencies {
    // Test doubles implement the shared contracts, so consumers get them transitively.
    api(project(":lib:tasks-api"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
