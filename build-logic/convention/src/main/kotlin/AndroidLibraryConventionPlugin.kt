import com.android.build.api.dsl.LibraryExtension
import com.rounds.test.to_dolist.convention.configureAndroidLibrary
import com.rounds.test.to_dolist.convention.configureKotlinToolchain
import com.rounds.test.to_dolist.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Baseline for every non-application module: SDK levels, JVM target, lint, unit test defaults. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> { configureAndroidLibrary(this) }
        configureKotlinToolchain()

        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
        }
    }
}
