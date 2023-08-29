FROM openjdk:17-alpine
MAINTAINER isKONSTANTIN <me@knst.su>

EXPOSE 8080

WORKDIR /finwave

COPY FinTrack.jar ./

ENTRYPOINT ["java", "-jar", "FinTrack.jar"]