buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // provide classes now missing in JDK 21
        classpath("com.sun.activation:javax.activation:1.2.0")
    }
}

plugins {
    base
    id("com.android.library") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jreleaser") version "1.19.0"
}

group = "org.rgbtools"
version = "0.3.0-beta.6"

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

jreleaser {
    project {
        name.set("rgb-lib-kotlin")
        description.set("RGB Lib Kotlin language bindings")
        longDescription.set("RGB Lib Kotlin language bindings for Android development")
        website.set("https://github.com/RGB-Tools/rgb-lib-kotlin")
        authors.set(listOf("Zoe Faltibà", "Nicola Busanello"))
        license.set("MIT")
        licenseUrl.set("https://spdx.org/licenses/MIT.html")
        copyright.set("2022-2026")

        java {
            groupId.set("org.rgbtools")
            version.set("11")
        }
    }

    release {
        github {
            // Keep enabled to satisfy JReleaser core requirements
            // but we won't actually use it since we only run jreleaserDeploy
            enabled.set(true)
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        // change signing mode from MEMORY (default) to COMMAND
        mode.set(org.jreleaser.model.Signing.Mode.COMMAND)

        command {
            // set GPG key + passphrase to be used for signing, from Gradle properties
            keyName.set(providers.gradleProperty("jreleaser.gpg.keyName"))
            passphrase.set(providers.gradleProperty("jreleaser.gpg.passphrase"))
        }
    }

    deploy {
        maven {
            // Disable POM checker for Android AAR packaging
            pomchecker {
                failOnError.set(false)
                failOnWarning.set(false)
            }

            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("android/build/staging-deploy")
                    // Disable POM verification for AAR packaging
                    verifyPom.set(false)
                }
            }
        }
    }
}
