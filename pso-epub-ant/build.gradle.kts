description = "ANT tasks and definition for EPUB API"

dependencies {
    compileOnly(project(":pso-epub-core"))
    compileOnly(libs.ant)

    testImplementation(platform(libs.junit))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.xmlunit)
    testImplementation(project(":pso-epub-core"))
    testImplementation(libs.ant)
    testImplementation(libs.epubcheck)
    testRuntimeOnly(libs.saxon)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    // epubcheck (Jing) compiles its RelaxNG schemas recursively and can overflow the default stack
    jvmArgs("-Xss8m")
    // EPUBs produced by the export tests are kept here for manual testing in a reader
    systemProperty("epub.test.output", layout.buildDirectory.dir("test-output/export").get().asFile.absolutePath)
}
