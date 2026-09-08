plugins {
    id("todo.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Navigation Compose serialises type-safe routes, so the contract needs the serialization runtime.
    implementation(libs.kotlinx.serialization.json)
}
