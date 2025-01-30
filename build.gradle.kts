plugins {
    application
    java
    alias(libs.plugins.shadow)
}

group = "de.donnerbart"
version = "0.1.7"

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
    useJUnitPlatform()
}
