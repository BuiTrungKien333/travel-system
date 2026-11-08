FROM postgres:17-alpine

ARG PGVECTOR_VERSION=0.8.2

# Keep the Alpine/PostgreSQL 17 base used by the existing dev data volume.
# PGXS is supplied by the official image; LLVM bitcode is not needed at runtime.
RUN apk add --no-cache --virtual .pgvector-build-deps build-base git \
    && git clone --depth 1 --branch "v${PGVECTOR_VERSION}" https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && make -C /tmp/pgvector OPTFLAGS="" with_llvm=no \
    && make -C /tmp/pgvector install with_llvm=no \
    && apk del .pgvector-build-deps \
    && rm -rf /tmp/pgvector
