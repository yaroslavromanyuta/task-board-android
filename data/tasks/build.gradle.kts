plugins {
    id("todo.android.library")
    id("todo.android.hilt")
}

android {
    namespace = "com.rounds.test.to_dolist.data.tasks"
}

dependencies {
    implementation(project(":lib:tasks-api"))
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
