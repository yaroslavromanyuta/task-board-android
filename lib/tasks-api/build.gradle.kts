plugins {
    id("todo.jvm.library")
}

dependencies {
    // Use cases are constructor-injected, but the module itself stays free of Hilt/Android.
    implementation(libs.javax.inject)

    // The shared test double implements this module's own contract, so it is consumed test-only.
    testImplementation(project(":core:testing"))
}
