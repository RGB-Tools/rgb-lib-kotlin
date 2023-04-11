FROM rust:1.69-slim-bullseye

RUN mkdir -p /usr/share/man/man1 \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        cmake g++ git gosu libpcre3-dev libssl-dev make openjdk-11-jdk \
        pkg-config unzip wget \
    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ARG USER="rgb-lib-kotlin"
ENV USER="$USER" USER_HOME="/home/$USER"
RUN adduser --home "$USER_HOME" --disabled-login --gecos "$USER user" $USER

ARG NDK_VERSION="21.4.7075529"
ENV ANDROID_SDK_ROOT="$USER_HOME/sdk"
ENV ANDROID_NDK_ROOT="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"

USER "$USER"

RUN cd && mkdir android-cli && cd android-cli \
    && wget https://dl.google.com/android/repository/commandlinetools-linux-9123335_latest.zip \
    && unzip commandlinetools-linux-*_latest.zip \
    && mkdir $ANDROID_SDK_ROOT \
    && mv cmdline-tools $ANDROID_SDK_ROOT/ \
    && cd && rm -rf android-cli

RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_SDK_ROOT \
        "platform-tools" \
        "build-tools;33.0.1" \
        "platforms;android-33" \
        "ndk;$NDK_VERSION" \
        "cmake;3.22.1"

RUN rustup update && rustup default stable
RUN rustup target add \
        aarch64-linux-android \
        x86_64-linux-android \
        armv7-linux-androideabi

COPY . $USER_HOME/rgb-lib-kotlin

WORKDIR $USER_HOME/rgb-lib-kotlin

RUN ./gradlew -v

USER root

COPY entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
