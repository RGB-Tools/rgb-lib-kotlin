# RGB Lib Kotlin bindings

This project builds an Android library, `rgb-lib-android`, for the [rgb-lib]
Rust library, which is included as a git submodule. The bindings are created by
the [rgb-lib-ffi] project, which in located inside the rgb-lib submodule.

## Usage

To use the Kotlin library add the following to your project gradle dependencies:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.rgbtools:rgb-lib-android:<version>'
}
```

## Contributing

### Build

In order to build the project, setup the following environment variables:
- `ANDROID_SDK_ROOT` (e.g. `export ANDROID_SDK_ROOT=~/Android/Sdk`)
- `ANDROID_NDK_ROOT` (e.g. `export ANDROID_NDK_ROOT=$ANDROID_SDK_ROOT/ndk/21.<NDK_VERSION>`)

Then, clone this project and run:

```bash
git submodule update --init
./gradlew :android:buildAndroidLib
```

### Publish

#### To local Maven repository

In order to publish the library to your local Maven repository:

```bash
./gradlew :android:publishToMavenLocal --exclude-task signMavenPublication
```

#### To Maven Central repository (project maintainers only)

Set your `~/.gradle/gradle.properties` signing key values and SONATYPE login
```properties
signingKey=<YOUR_GNUPG_ID_LAST_8_CHARS>
signingPassword=<YOUR_GNUPG_PASSPHRASE>
signing.gnupg.keyName=<YOUR_GNUPG_ID>

ossrhUsername=<YOUR_SONATYPE_USERNAME>
ossrhPassword=<YOUR_SONATYPE_PASSWORD>
```
and then publish by running:
```shell
./gradlew :android:publishToSonatype closeAndReleaseSonatypeStagingRepository
```


[rgb-lib]: https://github.com/RGB-Tools/rgb-lib
[rgb-lib-ffi]: https://github.com/RGB-Tools/rgb-lib/tree/master/rgb-lib-ffi
