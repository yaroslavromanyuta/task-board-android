plugins {
    id("todo.jvm.library")
}

dependencies {
    // Test doubles implement the shared contracts, so consumers get them transitively.
    api(project(":lib:tasks-api"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
