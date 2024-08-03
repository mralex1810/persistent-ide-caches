import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("antlr")
    kotlin("jvm") version "2.0.0"
}

group = "com.github"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val junitJupiterVersion = "5.9.2"
val junitPlatformVersion = "1.9.2"
val antlrPluginVersion = "4.13.1"
val sfl4jVersion = "2.0.6"

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.junit.platform:junit-platform-suite:$junitPlatformVersion")

    testImplementation("org.mockito:mockito-core:5.2.0")

    implementation("org.lmdbjava:lmdbjava:0.8.3")

    implementation("org.slf4j:slf4j-api:$sfl4jVersion")
    implementation("org.slf4j:slf4j-reload4j:$sfl4jVersion")

    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    // https://mvnrepository.com/artifact/org.antlr/antlr4-runtime
    implementation("org.antlr:antlr4-runtime:$antlrPluginVersion")
    antlr("org.antlr:antlr4:$antlrPluginVersion")

    implementation(kotlin("stdlib-jdk8"))
}
val generatedModule = "${projectDir}/src/main/java"
val generationOutput = "$generatedModule/com/github/sudu/persistentidecaches/javaparaser"

sourceSets {
    main {
        java {
            srcDir(generatedModule)
        }
    }
}

tasks.named<AntlrTask>("generateGrammarSource") {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-long-messages", "-package", "com.github.sudu.persistentidecaches.javaparaser"))
    outputDirectory = file(generationOutput)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.github.sudu.persistentidecaches.VsCodeClient"
    }

    // Ensure the runtime classpath also includes ANTLR generated sources
//    from(configurations.runtimeClasspath.map { if (it.isDirectory) it else zipTree(it) })

    archiveFileName.set("${project.name}-${project.version}-all.jar")
}

tasks.withType<JavaCompile>{
    dependsOn("generateGrammarSource")
}

tasks.withType<KotlinCompile> {
    dependsOn("generateGrammarSource")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.clean {
    doLast {
        fileTree(generationOutput).dir.deleteRecursively()
    }

}