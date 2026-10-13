plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("com.lagradost.cloudstream3.gradle") version "0.1.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}
