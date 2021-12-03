import ru.cometrica.gradle.ModuleType

plugins {
    id("gradle-configuration-plugin2")
}

dependencies {
    implementation("com.scottyab:rootbeer-lib:0.1.0")
}

moduleConfig{
    type = ModuleType.FEATURE
}