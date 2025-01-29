plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

group = "moe.mukjep.karamell"
version = "0.1.0-SNAPSHOT"

tasks.jar {
    archiveBaseName = project.name
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = version
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
