description = "Core EPUB API"

dependencies {
    compileOnly(libs.saxon)

    testImplementation(platform(libs.junit))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
