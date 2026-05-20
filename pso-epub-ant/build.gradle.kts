description = "ANT tasks and definition for EPUB API"

dependencies {
    compileOnly(project(":pso-epub-core"))
    compileOnly(libs.ant)

    testImplementation(platform(libs.junit))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.xmlunit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
