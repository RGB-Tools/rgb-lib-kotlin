package org.rgbtools.plugins

import org.gradle.kotlin.dsl.register

val llvmArchPath = when (operatingSystem) {
    OS.MAC   -> "darwin-x86_64"
    OS.LINUX -> "linux-x86_64"
    OS.OTHER -> throw Error("Cannot build Android library from current architecture")
}

val androidPath = "${project.projectDir}/../android"
val rgbLibUniffiPath = "${project.projectDir}/../rgb-lib/bindings/uniffi"

val jniLibsDir = "$androidPath/src/main/jniLibs/"

val androidNdkRoot = System.getenv("ANDROID_NDK_ROOT")

val cargoBuildCommonArgs: MutableList<String> = mutableListOf("build", "--release", "--target")

val prepareBuild by tasks.register("prepareBuild") {
    File(jniLibsDir).deleteRecursively()
}

// arm64-v8a is the most popular hardware architecture for Android
val buildAndroidAarch64Binary by tasks.register<Exec>("buildAndroidAarch64Binary") {

    workingDir(rgbLibUniffiPath)
    val cargoArgs = cargoBuildCommonArgs + mutableListOf("aarch64-linux-android")

    executable("cargo")
    args(cargoArgs)

    // if ANDROID_NDK_ROOT is not set then set it to github actions default
    if (System.getenv("ANDROID_NDK_ROOT") == null) {
        environment(
            Pair("ANDROID_NDK_ROOT", "${System.getenv("ANDROID_SDK_ROOT")}/ndk-bundle")
        )
    }

    environment(
        // add build toolchain to PATH
        Pair("PATH", "${System.getenv("PATH")}:${System.getenv("ANDROID_NDK_ROOT")}/toolchains/llvm/prebuilt/$llvmArchPath/bin"),

        Pair("CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER", "aarch64-linux-android21-clang"),
        Pair("CC", "aarch64-linux-android21-clang"),
        Pair("AR_aarch64_linux_android", "llvm-ar"),
        Pair("RANLIB_aarch64_linux_android", "llvm-ranlib"),

        // support 16KB pages
        Pair("RUSTFLAGS", "-C link-arg=-Wl,-z,max-page-size=16384"),
    )

    doLast {
        println("Native library for rgb-lib-android on aarch64 built successfully")
    }
}

// the x86_64 version of the library is mostly used by emulators
val buildAndroidX86_64Binary by tasks.register<Exec>("buildAndroidX86_64Binary") {

    workingDir(rgbLibUniffiPath)
    val cargoArgs = cargoBuildCommonArgs + mutableListOf("x86_64-linux-android")

    executable("cargo")
    args(cargoArgs)

    // if ANDROID_NDK_ROOT is not set then set it to github actions default
    if (System.getenv("ANDROID_NDK_ROOT") == null) {
        environment(
            Pair("ANDROID_NDK_ROOT", "${System.getenv("ANDROID_SDK_ROOT")}/ndk-bundle")
        )
    }

    environment(
        // add build toolchain to PATH
        Pair("PATH", "${System.getenv("PATH")}:${System.getenv("ANDROID_NDK_ROOT")}/toolchains/llvm/prebuilt/$llvmArchPath/bin"),

        Pair("CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER", "x86_64-linux-android21-clang"),
        Pair("CC", "x86_64-linux-android21-clang"),
        Pair("AR_x86_64_linux_android", "llvm-ar"),
        Pair("RANLIB_x86_64_linux_android", "llvm-ranlib"),

        // support 16KB pages
        Pair("RUSTFLAGS", "-C link-arg=-Wl,-z,max-page-size=16384"),
    )

    doLast {
        println("Native library for rgb-lib-android on x86_64 built successfully")
    }
}

// armeabi-v7a version of the library for older 32-bit Android hardware
val buildAndroidArmv7Binary by tasks.register<Exec>("buildAndroidArmv7Binary") {

    workingDir(rgbLibUniffiPath)
    val cargoArgs = cargoBuildCommonArgs + mutableListOf("armv7-linux-androideabi")

    executable("cargo")
    args(cargoArgs)

    // if ANDROID_NDK_ROOT is not set then set it to github actions default
    if (System.getenv("ANDROID_NDK_ROOT") == null) {
        environment(
            Pair("ANDROID_NDK_ROOT", "${System.getenv("ANDROID_SDK_ROOT")}/ndk-bundle")
        )
    }

    environment(
        // add build toolchain to PATH
        Pair("PATH", "${System.getenv("PATH")}:${System.getenv("ANDROID_NDK_ROOT")}/toolchains/llvm/prebuilt/$llvmArchPath/bin"),

        Pair("CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER", "armv7a-linux-androideabi21-clang"),
        Pair("CC", "armv7a-linux-androideabi21-clang"),
        Pair("AR_armv7_linux_androideabi", "llvm-ar"),
        Pair("RANLIB_armv7_linux_androideabi", "llvm-ranlib"),

        // support 16KB pages
        Pair("RUSTFLAGS", "-C link-arg=-Wl,-z,max-page-size=16384"),
    )

    doLast {
        println("Native library for rgb-lib-android on armv7 built successfully")
    }
}

// move the native libs build by cargo from rgb-lib/bindings/uniffi/target/<architecture>/release/
// to their place in the rgb-lib-android library
// the task only copies the available binaries built using the buildAndroid<architecture>Binary tasks
val moveNativeAndroidLibs by tasks.register<Copy>("moveNativeAndroidLibs") {

    into(jniLibsDir)

    into("arm64-v8a") {
        from("$rgbLibUniffiPath/target/aarch64-linux-android/release/librgblibuniffi.so")
        from("$androidNdkRoot/sources/cxx-stl/llvm-libc++/libs/arm64-v8a/libc++_shared.so")
    }

    into("x86_64") {
        from("$rgbLibUniffiPath/target/x86_64-linux-android/release/librgblibuniffi.so")
        from("$androidNdkRoot/sources/cxx-stl/llvm-libc++/libs/x86_64/libc++_shared.so")
    }

    into("armeabi-v7a") {
        from("$rgbLibUniffiPath/target/armv7-linux-androideabi/release/librgblibuniffi.so")
        from("$androidNdkRoot/sources/cxx-stl/llvm-libc++/libs/armeabi-v7a/libc++_shared.so")
    }

    doLast {
        println("Native binaries for Android moved to ./android/src/main/jniLibs/")
    }
}

// generate the bindings using the rgb-lib-uniffi-bindgen tool located in the rgb-lib submodule
val generateAndroidBindings by tasks.register<Exec>("generateAndroidBindings") {
    dependsOn(moveNativeAndroidLibs)

    workingDir(rgbLibUniffiPath)
    executable("cargo")
    args("run", "--bin", "rgb-lib-uniffi-bindgen", "generate", "src/rgb-lib.udl",
        "--language", "kotlin", "--out-dir", "$androidPath/src/main/kotlin")

    doLast {
        println("Android bindings file successfully created")
    }
}

// create an aggregate task which will run the required tasks to build the Android libs in order
// the task will also appear in the printout of the ./gradlew tasks task with group and description
tasks.register("buildAndroidLib") {
    group = "RgbTools"
    description = "Aggregate task to build Android library"

    dependsOn(
        prepareBuild,
        buildAndroidAarch64Binary,
        buildAndroidX86_64Binary,
        buildAndroidArmv7Binary,
        moveNativeAndroidLibs,
        generateAndroidBindings
    )
}
