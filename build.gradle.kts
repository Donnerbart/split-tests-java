plugins {
    application
    java
    alias(libs.plugins.gradle.git.properties)
    alias(libs.plugins.shadow)
}

group = "de.donnerbart"
version = "0.1.20"

application {
    mainClass = "de.donnerbart.split.TestSplitMain"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// ********** dependencies **********

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.java.parser)
    implementation(libs.jcommander)
    implementation(libs.logback.classic)
}

// ********** distribution **********

tasks.shadowJar {
    dependsOn(tasks.generateGitProperties)
    mergeServiceFiles()
    archiveBaseName = "split-tests-java"
    archiveClassifier = ""
    archiveVersion = ""
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "de.donnerbart.split.TestSplitMain"
    }
}

// ********** tests **********

dependencies {
    testCompileOnly(libs.jetbrains.annotations)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.assertj)
    testImplementation(libs.equalsVerifier)
}

tasks.test {
    dependsOn(tasks.generateGitProperties)
    useJUnitPlatform()
}

// ********** git properties **********

gitProperties {
    dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
    gitPropertiesName = "split-tests-java.properties"
    keys = listOf("git.branch", "git.commit.id", "git.commit.id.abbrev", "git.commit.time")
    customProperty("version", version)
    extProperty = "gitProps"
}
tasks.generateGitProperties {
    outputs.upToDateWhen { false }
}
