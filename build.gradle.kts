import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
    alias(libs.plugins.versions)
    alias(libs.plugins.sonar)
    alias(libs.plugins.jreleaser)
}

val title: String by project
val gitName: String by project
val website: String by project
val appVersion = file("version.txt").readText(Charsets.UTF_8).trim()

group = "org.pageseeder.epub"
version = appVersion

sonar {
    properties {
        property("sonar.projectKey", "pageseeder_pso-epub")
        property("sonar.organization", "pageseeder")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "**/build/reports/jacoco/test/jacocoTestReport.xml"
        )
    }
}

tasks.wrapper {
    gradleVersion = "8.14"
    distributionType = Wrapper.DistributionType.BIN
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")

    group = "org.pageseeder.epub"
    version = rootProject.version

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set(title)
                    description.set(project.description)
                    url.set(website)
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    organization {
                        name.set("Allette Systems")
                        url.set("https://www.allette.com.au")
                    }
                    scm {
                        url.set("git@github.com:pageseeder/${gitName}.git")
                        connection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
                        developerConnection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
                    }
                    developers {
                        developer {
                            name.set("Jean-Baptiste Reure")
                            email.set("jbreure@weborganic.com")
                        }
                        developer {
                            name.set("Philip Rutherford")
                            email.set("philipr@weborganic.com")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
            }
        }
    }
}

jreleaser {
    configFile.set(file("jreleaser.toml"))
    distributions {
        subprojects.forEach { subproject ->
            register(subproject.name) {
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}.jar"))
                }
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-sources.jar"))
                }
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-javadoc.jar"))
                }
            }
        }
    }
}
