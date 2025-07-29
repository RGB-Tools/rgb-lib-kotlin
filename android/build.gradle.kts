plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")

    // Custom plugin to generate the native libs and bindings file
    id("org.rgbtools.plugins.generate-android-bindings")
}

android {
    namespace = "org.rgbtools"

    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            proguardFiles(file("proguard-android-optimize.txt"), file("proguard-rules.pro"))
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.16.0")
    api("org.slf4j:slf4j-api:2.0.17")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "org.rgbtools"
                artifactId = "rgb-lib-android"
                version = "0.3.0-beta.2"
                from(components["release"])

                pom {
                    name.set("rgb-lib-android")
                    description.set("RGB Lib Kotlin language bindings.")
                    url.set("https://github.com/RGB-Tools/rgb-lib-kotlin")
                    inceptionYear.set("2022")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://spdx.org/licenses/MIT.html")
                        }
                    }
                    developers {
                        developer {
                            id.set("zoedberg")
                            name.set("Zoe Faltibà")
                            email.set("zoefaltiba@gmail.com")
                        }
                        developer {
                            id.set("nicbus")
                            name.set("Nicola Busanello")
                            email.set("nicola.busanello@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/RGB-Tools/rgb-lib-kotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/RGB-Tools/rgb-lib-kotlin.git")
                        url.set("https://github.com/RGB-Tools/rgb-lib-kotlin")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "staging"
                url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
            }
        }
    }
}

