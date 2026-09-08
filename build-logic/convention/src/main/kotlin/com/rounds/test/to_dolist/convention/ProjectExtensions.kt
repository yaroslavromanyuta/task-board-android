package com.rounds.test.to_dolist.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Single source of truth for the SDK levels; every module goes through [configureAndroidLibrary]. */
internal object AndroidSdk {
    const val COMPILE = 37
    const val TARGET = 37

    /** The brief asks for API 26+, so 26 is the floor rather than the template's 24. */
    const val MIN = 26
}

internal const val JVM_TARGET = 17

/**
 * Convention plugins cannot use the generated `libs.` accessors, so the catalog is looked up by name.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureAndroidLibrary(extension: LibraryExtension) = with(extension) {
    compileSdk {
        version = release(AndroidSdk.COMPILE)
    }

    defaultConfig {
        minSdk = AndroidSdk.MIN
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        // Makes lint follow project dependencies, so a module boundary violation surfaces here too.
        checkDependencies = true
    }

}

/** AGP 9 ships Kotlin built in, so the extension is looked up rather than added by a separate plugin. */
internal fun Project.configureKotlinToolchain() {
    extensions.configure(KotlinAndroidProjectExtension::class.java) { jvmToolchain(JVM_TARGET) }
}
