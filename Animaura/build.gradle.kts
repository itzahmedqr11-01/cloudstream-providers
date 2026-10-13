plugins {
    kotlin("jvm")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    // Generates Animaura.cs3
    setRepo("https://github.com/username/cloudstream-animaura-stardima")
    authors = listOf("Cloudstream Dev")
    status = 1 // 1: Beta/Working, 2: Stable
    description = "Provider for Animaura anime and Miraculous Ladybug universe."
}

dependencies {
    // CloudStream API dependencies injected by the gradle plugin
}
