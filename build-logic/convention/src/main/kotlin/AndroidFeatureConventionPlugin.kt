import com.rounds.test.to_dolist.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module is a screen: Compose + Hilt + the only dependencies the boundary rules allow,
 * namely `:core:*` and `:lib:*`. Nothing here reaches `:data:*` or another feature.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("todo.android.library.compose")
        pluginManager.apply("todo.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:common"))
            add("implementation", project(":lib:tasks-api"))
            add("implementation", project(":lib:navigation-api"))

            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-savedstate").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())

            add("testImplementation", project(":core:testing"))
        }
    }
}
