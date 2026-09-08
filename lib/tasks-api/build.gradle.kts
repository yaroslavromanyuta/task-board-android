plugins {
    id("todo.jvm.library")
}

dependencies {
    // Use cases are constructor-injected, but the module itself stays free of Hilt/Android.
    implementation(libs.javax.inject)
}
