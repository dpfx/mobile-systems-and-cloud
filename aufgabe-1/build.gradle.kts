// Plugins are declared here and applied in the modules that need them.
// No kotlin-android plugin: since AGP 9, Kotlin support is built into the Android plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
