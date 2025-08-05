# RGB Lib Kotlin bindings

This project builds an Android library, `rgb-lib-android`, for the [rgb-lib]
Rust library, which is included as a git submodule. The bindings are created by
the [rgb-lib-uniffi] project, which is located inside the rgb-lib submodule.

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

Clone the project, including submodules:

```bash
git clone git@github.com:rgb-tools-devs/rgb-lib-kotlin.git --recurse-submodules
```

When fetching updates, remember to update the submodule as well:

```bash
git submodule update --init
```

### Build

#### In docker

Build the docker image (if your user or group IDs are not `1000`, adjust the
environment variables `MYUID` and `MYGID` in the `compose.yaml` file
accordingly):

```bash
docker compose run --build --rm builder
```

Notes:

- image build takes a long time and uses a lot of disk space
- the local directory will be mounted into the container at runtime
- the `gradle_cache` local directory will be mounted into the container to
  cache gradle downloads

#### Local

Setup the following environment variables:

- `ANDROID_SDK_ROOT` (e.g. `export ANDROID_SDK_ROOT=$HOME/Android/Sdk`)
- `ANDROID_NDK_ROOT` (`export ANDROID_NDK_ROOT=$ANDROID_SDK_ROOT/ndk/25.<NDK_VERSION>`)

Note: NDK version 25.2.x can be installed via android studio (SDK Manager) or
command-line tools (sdkmanager).

Add the required Android rust targets:

```bash
rustup target add aarch64-linux-android
rustup target add x86_64-linux-android
```

Build the Android library:

```bash
./gradlew :android:buildAndroidLib
```

### Page alignment

Check the build library page alignment:

```bash
for LIB in $(find rgb-lib/bindings/uniffi/target/ -wholename '*release/librgblibuniffi.so'); do ls -l "$LIB"; readelf -l "$LIB" |grep -A1 LOAD; done
```

The last column of each LOAD row is the alignment:
- 0x1000 => pages align to 4KB
- 0x4000 => pages align to 16KB

### Publish

Download [OpenJDK 18] and unpack it to a directory of your choice (e.g.
`$HOME/jdk`).

Setup the following environment variables:

- `ANDROID_SDK_ROOT` (e.g. `export ANDROID_SDK_ROOT="$HOME/Android/Sdk"`)
- `ANDROID_HOME` (`export ANDROID_HOME="$ANDROID_SDK_ROOT"`)
- `ANDROID_NDK_ROOT` (`export ANDROID_NDK_ROOT=$ANDROID_SDK_ROOT/ndk/25.2.<NDK_VERSION>`)
- `JAVA_HOME` (e.g. `export JAVA_HOME=$HOME/jdk/jdk-18.0.2`)
- `PATH` (`export PATH=$JAVA_HOME/bin:$PATH`)

#### To local Maven repository

Publish the library to your local Maven repository:

```bash
./gradlew :android:publishToMavenLocal
```

#### To Maven Central repository (project maintainers only)

Create a `~/.jreleaser/config.yml` file with the publishing and signing information:

```yaml
JRELEASER_GITHUB_OWNER: RGB-Tools
JRELEASER_GITHUB_NAME: rgb-lib-kotlin
JRELEASER_GITHUB_TOKEN: fake-token-not-used

JRELEASER_MAVENCENTRAL_USERNAME: "<your-publisher-portal-username>"
JRELEASER_MAVENCENTRAL_PASSWORD: "<your-publisher-portal-password>"
```

Test your configuration by running:
```shell
./gradlew jreleaserConfig
```

Build the artifacts by running:
```shell
./gradlew :android:publish
```

Set your GPG password to the apprpriate environment variable by running:
```shell
read -rsp "GPG password: " JRELEASER_GPG_PASSPHRASE
```

Test the publishing by running:
```shell
./gradlew jreleaserDeploy --dryrun -Pjreleaser.gpg.keyName="<your_key_id>" -Pjreleaser.gpg.passphrase="$JRELEASER_GPG_PASSPHRASE"
```

Publish by running:
```shell
./gradlew jreleaserDeploy -Pjreleaser.gpg.keyName="<your_key_id>" -Pjreleaser.gpg.passphrase="$JRELEASER_GPG_PASSPHRASE"
```

[rgb-lib]: https://github.com/RGB-Tools/rgb-lib
[rgb-lib-uniffi]: https://github.com/RGB-Tools/rgb-lib/tree/master/bindings/uniffi
[OpenJDK 18]: (https://jdk.java.net/archive/)
