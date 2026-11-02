# Cantaloupe IIIF Image API server for Rosa, built for Amazon ECS (linux/amd64).
#
# Four build stages produce the pieces the runtime image needs, so that no
# compiler, no source tree, and in particular no Kakadu source ends up in the
# published image:
#
#   0. kakadu_src   - the licensed Kakadu ZIP (this directory, or the named
#                     build context that replaces it)
#   1. kakadu       - compiles the commercial Kakadu distribution (JNI binding,
#                     shared libraries, command-line tools)
#   2. turbojpeg    - compiles libjpeg-turbo *with Java support*, which distro
#                     packages omit and which Cantaloupe's bundled TurboJPEG
#                     Java binding requires
#   3. cantaloupe   - downloads the GitHub release ZIP and extracts the jar
#
# The final, unnamed stage assembles them into the runtime image.
#
# Build (preferred; see build-and-push.sh):
#   ./build-and-push.sh --kakadu-zip /path/to/v8_6_1-02130L.zip
#
# which runs the equivalent of:
#   docker build --platform linux/amd64 -t rosa-image-server:5.0.7 \
#     --build-context kakadu_src=/dir/containing/zip \
#     --build-arg KAKADU_ZIP=v8_6_1-02130L.zip .
#
# The named build context "kakadu_src" replaces the stage of the same name
# below, so the licensed ZIP never has to be copied into this repository.
# Without --build-context the ZIP is read from this directory instead.

# Base image, pinned to the multi-arch index digest of the amazoncorretto
# 25-al2023-jdk tag so that a rebuild is reproducible. To move to a newer
# Corretto, fetch the current digest without Docker:
#   TOKEN=$(curl -fsSL 'https://auth.docker.io/token?service=registry.docker.io&scope=repository:library/amazoncorretto:pull' | jq -r .token)
#   curl -fsSI -H "Authorization: Bearer $TOKEN" \
#     -H 'Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json' \
#     https://registry-1.docker.io/v2/library/amazoncorretto/manifests/25-al2023-jdk | grep -i docker-content-digest
# (or `docker buildx imagetools inspect amazoncorretto:25-al2023-jdk`).
ARG JAVA_IMAGE=amazoncorretto:25-al2023-jdk@sha256:8806f69d6997672a37569da54378d13ff2cd613f9abb151c493f5a359a0a8676
ARG KAKADU_ZIP=v8_6_1-02130L.zip

# Cantaloupe release. This is the single place the version is set; the
# runtime stage re-declares the ARG without a default so it inherits this
# value, and build-and-push.sh reads this line for its default tag.
ARG CANTALOUPE_VERSION=5.0.7
# SHA-256 of cantaloupe-${CANTALOUPE_VERSION}.zip. Update together with the
# version: `curl -fsSL -o c.zip <release url> && sha256sum c.zip`.
ARG CANTALOUPE_SHA256=741ddee3f5284b179cd2555dc545a8afcb02534f03896cfa9235249fd66b0665

# libjpeg-turbo source tarball (GitHub tag archive) and its SHA-256.
# The Cantaloupe manual specifies 2.0.2; later 2.0.x releases may work but
# have not been tested here. Do not jump to 3.x: its TurboJPEG Java API
# differs from the classes vendored in the Cantaloupe jar.
ARG LIBJPEG_TURBO_VERSION=2.0.2
ARG LIBJPEG_TURBO_SHA256=b45255bd476c19c7c6b198c07c0487e8b8536373b82f2b38346b32b4fa7bb942

###############################################################################
# Stage 0: Kakadu source ZIP
#
# Default source of the ZIP: this directory. Overridden wholesale by
# `--build-context kakadu_src=<dir>`, in which case <dir>/${KAKADU_ZIP} is used
# and this stage is never built.
###############################################################################
FROM scratch AS kakadu_src
ARG KAKADU_ZIP
COPY ${KAKADU_ZIP} /

###############################################################################
# Stage 1: Kakadu
#
# Builds the command-line tools into bin/Linux-x86-64-gcc and the shared
# libraries (libkdu_jni.so, libkdu_aVVR.so, libkdu_vVVR.so) into
# lib/Linux-x86-64-gcc. The JNI library is what Cantaloupe's
# KakaduNativeProcessor loads via System.loadLibrary("kdu_jni").
#
# The makefiles compile with -O2 -msse2 and gate SSSE3/SSE4/AVX/AVX2 code paths
# behind runtime CPUID tests, so the result runs on any x86-64 host.
###############################################################################
FROM ${JAVA_IMAGE} AS kakadu

ARG KAKADU_ZIP
ARG KAKADU_PLATFORM=Linux-x86-64-gcc

RUN dnf -y install gcc-c++ make unzip findutils \
 && dnf clean all \
 && rm -rf /var/cache/dnf

WORKDIR /build

# A commercial Kakadu license is required to build and run this. The
# distribution ZIP is supplied by the kakadu_src stage/context, never downloaded.
COPY --from=kakadu_src /${KAKADU_ZIP} kakadu.zip
RUN unzip -q kakadu.zip -d unpacked \
 && mv "$(find unpacked -mindepth 1 -maxdepth 1 -type d | head -1)" kakadu \
 && rm -rf unpacked kakadu.zip

# JAVA_HOME comes from the base image and must point at a JDK: the managed/
# subproject compiles against $JAVA_HOME/include/jni.h to produce libkdu_jni.so.
RUN test -f "${JAVA_HOME}/include/jni.h" \
 && cd kakadu/make \
 && make -f Makefile-${KAKADU_PLATFORM}

# Stage the artifacts, and fail the build now rather than at runtime if the
# JNI binding did not get built.
RUN mkdir -p /kakadu/bin /kakadu/lib \
 && cp kakadu/bin/${KAKADU_PLATFORM}/* /kakadu/bin/ \
 && cp kakadu/lib/${KAKADU_PLATFORM}/*.so /kakadu/lib/ \
 && cp kakadu/LICENSE.TXT /kakadu/LICENSE.TXT \
 && test -f /kakadu/lib/libkdu_jni.so \
 && test -x /kakadu/bin/kdu_expand

###############################################################################
# Stage 2: libjpeg-turbo with the Java (TurboJPEG JNI) binding
#
# Cantaloupe ships the org.libjpegturbo.turbojpeg Java classes inside its jar
# but not the native library, and TJLoader looks for libturbojpeg.so on the
# library path and then at /opt/libjpeg-turbo/lib64. Without it, JPEG encoding
# silently falls back to the slower Image I/O writer.
#
# The version and checksum are set at the top of this file.
###############################################################################
FROM ${JAVA_IMAGE} AS turbojpeg

ARG LIBJPEG_TURBO_VERSION
ARG LIBJPEG_TURBO_SHA256

RUN dnf -y install gcc make cmake nasm tar gzip \
 && dnf clean all \
 && rm -rf /var/cache/dnf

WORKDIR /build
RUN curl -fsSL -o libjpeg-turbo.tar.gz \
      "https://github.com/libjpeg-turbo/libjpeg-turbo/archive/refs/tags/${LIBJPEG_TURBO_VERSION}.tar.gz" \
 && echo "${LIBJPEG_TURBO_SHA256}  libjpeg-turbo.tar.gz" | sha256sum -c - \
 && tar xzf libjpeg-turbo.tar.gz \
 && cmake -S "libjpeg-turbo-${LIBJPEG_TURBO_VERSION}" -B /tmp/ljt-build \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX=/opt/libjpeg-turbo \
      -DCMAKE_INSTALL_DEFAULT_LIBDIR=lib64 \
      -DWITH_JAVA=1 \
 && cmake --build /tmp/ljt-build -j"$(nproc)" \
 && cmake --install /tmp/ljt-build \
 && test -f /opt/libjpeg-turbo/lib64/libturbojpeg.so \
 && nm -D --defined-only /opt/libjpeg-turbo/lib64/libturbojpeg.so \
      | grep -q Java_org_libjpegturbo_turbojpeg

###############################################################################
# Stage 3: Cantaloupe release jar
#
# Only the jar is kept. The rest of the release ZIP is discarded on purpose:
# its deps/ directory contains Kakadu binaries built under a Kakadu Public
# Service License that "may not be used commercially", and they would shadow
# the licensed build from stage 1.
###############################################################################
FROM ${JAVA_IMAGE} AS cantaloupe

ARG CANTALOUPE_VERSION
ARG CANTALOUPE_SHA256

RUN dnf -y install unzip findutils \
 && dnf clean all \
 && rm -rf /var/cache/dnf

WORKDIR /build
RUN curl -fsSL -o cantaloupe.zip \
      "https://github.com/cantaloupe-project/cantaloupe/releases/download/v${CANTALOUPE_VERSION}/cantaloupe-${CANTALOUPE_VERSION}.zip" \
 && echo "${CANTALOUPE_SHA256}  cantaloupe.zip" | sha256sum -c - \
 && unzip -q cantaloupe.zip -d unpacked \
 && mkdir -p /cantaloupe \
 && cp "$(find unpacked -name cantaloupe-${CANTALOUPE_VERSION}.jar -type f | head -1)" \
       /cantaloupe/cantaloupe.jar \
 && rm -rf unpacked cantaloupe.zip

###############################################################################
# Runtime image
###############################################################################
FROM ${JAVA_IMAGE}

ARG CANTALOUPE_VERSION

LABEL org.opencontainers.image.title="rosa-image-server" \
      org.opencontainers.image.description="Cantaloupe IIIF Image API server with commercial Kakadu JPEG 2000 support" \
      org.opencontainers.image.version="${CANTALOUPE_VERSION}"

# shadow-utils provides useradd. fontconfig/freetype are already present in the
# base image and are needed by Java 2D.
RUN dnf -y install shadow-utils \
 && dnf clean all \
 && rm -rf /var/cache/dnf

COPY --from=kakadu    /kakadu/lib/          /opt/kakadu/lib/
COPY --from=kakadu    /kakadu/bin/          /opt/kakadu/bin/
COPY --from=kakadu    /kakadu/LICENSE.TXT   /opt/kakadu/LICENSE.TXT
COPY --from=turbojpeg /opt/libjpeg-turbo/   /opt/libjpeg-turbo/
COPY --from=cantaloupe /cantaloupe/cantaloupe.jar /opt/cantaloupe/cantaloupe.jar

COPY cantaloupe.properties /etc/cantaloupe/cantaloupe.properties
COPY delegates.rb /etc/cantaloupe/delegates.rb

# ldconfig serves the kdu_* command-line tools, which link against
# libkdu_vVVR.so. The JVM ignores it, hence -Djava.library.path below.
RUN printf '/opt/kakadu/lib\n/opt/libjpeg-turbo/lib64\n' \
      > /etc/ld.so.conf.d/rosa-image-server.conf \
 && ldconfig \
 && ! ldd /opt/kakadu/bin/kdu_expand | grep -q 'not found'

RUN useradd --system --create-home --home-dir /var/lib/cantaloupe \
      --shell /sbin/nologin cantaloupe \
 && mkdir -p /var/cache/cantaloupe /var/tmp/cantaloupe \
 && chown -R cantaloupe:cantaloupe /var/cache/cantaloupe /var/tmp/cantaloupe

ENV PATH=/opt/kakadu/bin:${PATH} \
    CANTALOUPE_VERSION=${CANTALOUPE_VERSION} \
    CANTALOUPE_CONFIG=/etc/cantaloupe/cantaloupe.properties \
    JAVA_OPTS=""

# System.loadLibrary() searches java.library.path, which replaces rather than
# extends the default, so the standard JVM entries are repeated here.
ENV JAVA_LIBRARY_PATH=/opt/kakadu/lib:/opt/libjpeg-turbo/lib64:/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib

USER cantaloupe
WORKDIR /var/lib/cantaloupe

EXPOSE 8182

# Local convenience only; ECS uses the health check in the task definition.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8182/health || exit 1

# MaxRAMPercentage=75: the JVM otherwise picks ~25% of the container limit,
# which the Cantaloupe manual specifically calls out as too conservative. A
# later -XX:MaxRAMPercentage in JAVA_OPTS wins (jhu-rosa2-tf passes 65.0).
# --enable-native-access: Kakadu and TurboJPEG are loaded through
# System.loadLibrary(), which JDK 24+ reports as a restricted call and will
# block outright in a future release unless native access is granted.
# Per-deployment additions (heap tuning, GC choice) go in JAVA_OPTS.
ENTRYPOINT ["/bin/sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 --enable-native-access=ALL-UNNAMED -Djava.awt.headless=true -Djava.library.path=$JAVA_LIBRARY_PATH -Dcantaloupe.config=$CANTALOUPE_CONFIG $JAVA_OPTS -jar /opt/cantaloupe/cantaloupe.jar"]
