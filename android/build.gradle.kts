plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("signing")

    // Custom plugin to generate the native libs and bindings file
    id("org.rgbtools.plugins.generate-android-bindings")
}

android {
    namespace = "org.rgbtools"

    compileSdk = 33

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("androidx.core:core-ktx:1.9.0")
    api("org.slf4j:slf4j-api:2.0.6")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "org.rgbtools"
                artifactId = "rgb-lib-android"
                version = "0.2.0-alpha.2"
                from(components["release"])
                pom {
                    name.set("rgb-lib-android")
                    description.set("RGB Lib Kotlin language bindings.")
                    url.set("https://github.com/RGB-Tools/rgb-lib-kotlin")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://github.com/RGB-Tools/rgb-lib-kotlin/blob/master/LICENSE")
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
                        connection.set("scm:git:github.com/RGB-Tools/rgb-lib-kotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/RGB-Tools/rgb-lib-kotlin.git")
                        url.set("https://github.com/RGB-Tools/rgb-lib-kotlin")
                    }
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
