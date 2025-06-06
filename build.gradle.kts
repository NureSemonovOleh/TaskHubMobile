// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
ext {
    // Версії Androidx Navigation Component
    set("navigation_version", "2.7.7") // Оновлена версія Navigation

    // Версії Material Components та AppCompat
    set("material_version", "1.12.0") // Актуальна версія Material Components
    set("appcompat_version", "1.6.1") // Актуальна версія AppCompat
}