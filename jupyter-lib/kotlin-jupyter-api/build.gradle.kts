import com.jfrog.bintray.gradle.BintrayExtension
import java.util.Date
import kotlin.Suppress

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.0-rc"
    id("com.jfrog.bintray") version "1.8.1"
}

project.version = rootProject.version
val kotlinxSerializationVersion: String by rootProject
val publicationName = "api"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    testImplementation(kotlin("test"))

    val junitVersion = "5.6.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    @Suppress("UNUSED_VARIABLE")
    val sourceJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    dokkaHtml {
        outputDirectory = "$buildDir/dokka"
    }

    dokkaJavadoc {
        outputDirectory = this@tasks.javadoc.get().destinationDir!!.path
        inputs.dir("src/main/kotlin")
    }

    @Suppress("UNUSED_VARIABLE")
    val javadocJar by registering(Jar::class) {
        group = "documentation"
        dependsOn(dokkaJavadoc)
        archiveClassifier.set("javadoc")
        from(this@tasks.javadoc.get().destinationDir!!)
    }
}

publishing {
    publications {
        create<MavenPublication>(publicationName) {
            artifactId = "notebook-api"
            groupId = "org.jetbrains.kotlinx.jupyter"

            from(components["java"])
            artifact(tasks["sourceJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

bintray {

    // property must be set in ~/.gradle/gradle.properties
    user = project.findProperty("bintray_user") as String? ?: ""
    key = project.findProperty("bintray_key") as String? ?: ""
    val bintrayRepo = project.findProperty("bintray_repo") as String? ?: ""
    val bintrayUserOrg = project.findProperty("bintray_user_org") as String? ?: ""

    setPublications(publicationName) // When uploading configuration files

    dryRun = false // Whether to run this as dry-run, without deploying
    publish = true // If version should be auto published after an upload

    pkg(
        delegateClosureOf<BintrayExtension.PackageConfig> {
            repo = bintrayRepo
            name = "kotlin-jupyter-api"
            userOrg = bintrayUserOrg

            vcsUrl = project.findProperty("projectRepoUrl") as String? ?: ""

            setLicenses("MIT")
            publicDownloadNumbers = true

            // Optional version descriptor
            version(
                delegateClosureOf<BintrayExtension.VersionConfig> {
                    val projVersion = project.version as String
                    name = projVersion // Bintray logical version name
                    desc = "API for Kotlin Jupyter notebooks"
                    released = Date().toString()
                    vcsTag = projVersion
                }
            )
        }
    )
}
