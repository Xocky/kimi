// Корневой build-файл проекта: версии плагинов задаём один раз здесь
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Плагин Compose-компилятора (нужен начиная с Kotlin 2.0)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
