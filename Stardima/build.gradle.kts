plugins {
    kotlin("jvm")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    // Generates Stardima.cs3
    setRepo("https://github.com/username/cloudstream-animaura-stardima")
    authors = listOf("Cloudstream Dev")
    status = 1
    description = "Provider for Stardima Arabic Cartoons (cm.cartoon.com.im)."
}

dependencies {
    // CloudStream API dependencies injected by the gradle plugin
}
