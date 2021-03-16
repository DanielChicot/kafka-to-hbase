# Multi stage docker build - stage 1 builds jar file
FROM dwp-kotlin-slim-gradle-k2hb:latest as build

# Output folder
RUN mkdir -p /k2hb_builds

# Copy the gradle config and install dependencies
COPY build.gradle.kts .

# Copy the source
COPY src/ ./src

# Create DistTar
ARG K2HB_WRITE_TO_METADATA_STORE
RUN echo ${K2HB_WRITE_TO_METADATA_STORE}
RUN gradle :unit build -x test \
    && gradle distTar

RUN cp build/distributions/*.* /k2hb_builds/
RUN ls -la /k2hb_builds/

# Second build stage starts here
FROM openjdk:14-alpine

MAINTAINER DWP

COPY ./entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]
CMD ["./bin/kafka2hbase"]

ARG DIST_FILE=kafka2hbase-*.tar
ARG http_proxy_full=""

ENV APPLICATION=kafka2hbase
# Set user to run the process as in the docker contianer
ENV USER_NAME=k2hb
ENV GROUP_NAME=k2hb

# Create group and user to execute task
RUN addgroup ${GROUP_NAME}
RUN adduser --system --ingroup ${GROUP_NAME} ${USER_NAME}

# Add Aurora cert
RUN mkdir -p /certs /var/log/k2hb
COPY ./AmazonRootCA1.pem /certs/
RUN chown -R ${GROUP_NAME}:${USER_NAME} /certs /var/log/k2hb
RUN chmod -R a+rx /certs
RUN chmod 600 /certs/AmazonRootCA1.pem
RUN ls -la /certs

# Set environment variables for apk
ENV http_proxy=${http_proxy_full}
ENV https_proxy=${http_proxy_full}
ENV HTTP_PROXY=${http_proxy_full}
ENV HTTPS_PROXY=${http_proxy_full}

RUN echo "ENV http: ${http_proxy}" \
    && echo "ENV https: ${https_proxy}" \
    && echo "ENV HTTP: ${HTTP_PROXY}" \
    && echo "ENV HTTPS: ${HTTPS_PROXY}" \
    && echo "ARG full: ${http_proxy_full}" \
    && echo "DIST FILE: ${DIST_FILE}."
ENV CRYPTOGRAPHY_DONT_BUILD_RUST 1
ENV acm_cert_helper_version 0.38.0
RUN echo "===> Installing Dependencies ..." \
    && echo "===> Updating base packages ..." \
    && apk update \
    && apk upgrade \
    && echo "==Update done==" \
    && apk add --no-cache util-linux \
    && echo "===> Installing acm_pca_cert_generator ..." \
    && apk add --no-cache g++ python3 python3-dev libffi-dev openssl-dev gcc py3-pip rust cargo py3-cryptography \
    && pip3 install --upgrade pip setuptools wheel \
    && pip3 install https://github.com/dwp/acm-pca-cert-generator/releases/download/${acm_cert_helper_version}/acm_cert_helper-${acm_cert_helper_version}.tar.gz \
    && echo "==Dependencies done=="

WORKDIR /kafka2hbase

COPY --from=build /k2hb_builds/$DIST_FILE .

RUN tar -xf $DIST_FILE --strip-components=1
RUN chown ${USER_NAME}:${GROUP_NAME} . -R

USER $USER_NAME
