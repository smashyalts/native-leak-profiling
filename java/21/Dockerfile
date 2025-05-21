#
# Copyright (c) 2021 Matthew Penner
# (MIT License text omitted for brevity)
#

FROM ghcr.io/graalvm/graalvm-ce:21.0.2 AS builder

# Install build dependencies using microdnf
USER root
RUN microdnf update -y && \
    microdnf install -y \
        gcc \
        gcc-c++ \
        make \
        autoconf \
        git \
        graphviz \
        which \
        lsof \
        curl \
        openssl \
        tar \
        sqlite \
        fontconfig \
        freetype \
        tzdata \
        iproute \
        libstdc++ \
        shadow-utils && \
    microdnf clean all

RUN mkdir -p /opt /tmp/jprof /tmp/nmt /tmp/pmap /diagnostic

WORKDIR /opt
RUN git clone https://github.com/jemalloc/jemalloc.git && \
    cd jemalloc && \
    git checkout 5.3.0 && \
    ./autogen.sh --enable-prof && \
    make dist && \
    make && \
    make install

# Runtime image
FROM ghcr.io/graalvm/graalvm-ce:21.0.2

LABEL author="Matthew Penner" maintainer="matthew@pterodactyl.io"
LABEL org.opencontainers.image.source="https://github.com/pterodactyl/yolks"
LABEL org.opencontainers.image.licenses=MIT

# Install runtime dependencies with microdnf
USER root
RUN microdnf update -y && \
    microdnf install -y \
        screen \
        graphviz \
        lsof \
        curl \
        openssl \
        git \
        tar \
        sqlite \
        fontconfig \
        freetype \
        tzdata \
        iproute \
        libstdc++ \
        shadow-utils && \
    microdnf clean all && \
    useradd -m -d /home/container container

# Add jemalloc from builder image
COPY --from=builder /usr/local/lib/libjemalloc.so /usr/local/lib/libjemalloc.so
COPY ./../entrypoint.sh /entrypoint.sh

USER container
ENV USER=container \
    HOME=/home/container

WORKDIR /home/container/

CMD ["/bin/bash", "/entrypoint.sh"]
