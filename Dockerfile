FROM gradle:9.2.1-jdk24-alpine AS build
COPY . /app/
RUN cd /app && gradle -Dorg.gradle.welcome=never --no-daemon :action:bootJar

FROM ghcr.io/bell-sw/liberica-openjre-alpine:24
COPY --from=build /app/action/build/libs/action.jar /opt/action/action.jar
ENTRYPOINT ["java", "-jar", "/opt/action/action.jar"]
