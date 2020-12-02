import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.jupyter.build.getFlag
import org.jetbrains.kotlin.jupyter.plugin.options

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.jupyter.dependencies")
}

val kotlinxSerializationVersion: String by project
val junitVersion: String by project
val slf4jVersion: String by project
val khttpVersion: String by project

val taskOptions = project.options()
val deploy: Configuration by configurations.creating

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    val kotlinLanguageLevel: String by rootProject
    val jvmTarget: String by rootProject

    tasks.withType(KotlinCompile::class.java).all {
        kotlinOptions {
            languageVersion = kotlinLanguageLevel
            this.jvmTarget = jvmTarget
        }
    }

    repositories {
        maven { url = uri("https://kotlin.bintray.com/kotlin-dependencies") }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation(kotlin("test"))

    implementation(project(":kotlin-jupyter-deps"))
    implementation(project(":kotlin-jupyter-api"))
    implementation(project(":kotlin-jupyter-compiler"))
    implementation(project(":jupyter-lib"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("scripting-ide-services") as String) { isTransitive = false }
    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-compiler-embeddable"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("script-util"))
    implementation(kotlin("main-kts"))
    implementation(kotlin("serialization"))

    compileOnly(kotlin("scripting-compiler-impl"))

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("khttp:khttp:$khttpVersion")
    implementation("org.zeromq:jeromq:0.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.github.ajalt:clikt:2.8.0")
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")

    deploy(project(":jupyter-lib"))
    deploy(project(":kotlin-jupyter-api"))
    deploy(kotlin("script-runtime"))
}

tasks.register("publishLocal") {
    group = "publishing"

    dependsOn(
        "condaPackage",
        "pyPiPackage",
        ":kotlin-jupyter-api:publish",
        ":kotlin-jupyter-compiler:publish",
        ":kotlin-jupyter-deps:publish"
    )
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = taskOptions.mainClassFQN
        attributes["Implementation-Version"] = project.version
    }
}

tasks.shadowJar {
    archiveBaseName.set(taskOptions.packageName)
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes(tasks.jar.get().manifest.attributes)
    }
}

tasks.test {
    val doParallelTesting = getFlag("test.parallel", true)

    /**
     *  Set to true to debug classpath/shadowing issues, see testKlaxonClasspathDoesntLeak test
     */
    val useShadowedJar = getFlag("test.useShadowed", false)

    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    if (useShadowedJar) {
        dependsOn(tasks.shadowJar.get())
        classpath = files(tasks.shadowJar.get()) + classpath
    }

    systemProperties = mutableMapOf(
        "junit.jupiter.displayname.generator.default" to "org.junit.jupiter.api.DisplayNameGenerator\$ReplaceUnderscores",

        "junit.jupiter.execution.parallel.enabled" to doParallelTesting.toString() as Any,
        "junit.jupiter.execution.parallel.mode.default" to "concurrent",
        "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent"
    )
}

tasks.processResources {
    dependsOn(tasks.buildProperties)
}

tasks.check {
    dependsOn(tasks.checkReadme)
}
