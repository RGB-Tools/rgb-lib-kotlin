buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    }
}

plugins {
    id("signing")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

group = "org.rgbtools"
version = "0.2.0-alpha.3"

nexusPublishing {
    repositories {
        create("sonatype") {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            val ossrhUsername: String? by project
            val ossrhPassword: String? by project
            username.set(ossrhUsername)
            password.set(ossrhPassword)
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
