# syntax=docker/dockerfile:1
# One image definition for every service; pick the Maven module with --build-arg MODULE=<folder>.

FROM eclipse-temurin:17-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# The cache is shared by all service builds; locking keeps parallel builds from corrupting it.
RUN --mount=type=cache,id=maven-repository,target=/root/.m2,sharing=locked \
    ./mvnw -B -q -pl "${MODULE}" -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
