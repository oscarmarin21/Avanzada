# Dev container: Java 21, Maven, Node 20, Angular CLI
# Use 21-jdk (Ubuntu) for maximum compatibility with Docker Hub
FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Maven
ARG MAVEN_VERSION=3.9.6
RUN curl -sL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn
ENV MAVEN_HOME=/opt/maven

# Node 20 (LTS)
ARG NODE_VERSION=20
RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash - \
    && apt-get update \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*
RUN npm install -g @angular/cli@18

# Default workspace (override by compose mount)
WORKDIR /workspaces/Avanzada
