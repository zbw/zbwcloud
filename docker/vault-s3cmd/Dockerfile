# install vault
FROM alpine:latest AS vault
ARG VAULT_VERSION=1.12.2
RUN apk add --no-cache curl && \
    curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault* && \
    apk del curl

# install s3cmd
FROM vault AS s3cmd
RUN apk add --no-cache python3 py3-pip py3-setuptools libmagic && \
    pip3 install --no-cache-dir python-dateutil python-magic s3cmd

# use standard user
FROM s3cmd
ARG IMAGE_USER=vault-s3cmd-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER && \
    mkdir -p /var/log/$IMAGE_USER && \
    chown -R $IMAGE_USER:$IMAGE_USER /var/log/$IMAGE_USER
USER $IMAGE_USER


