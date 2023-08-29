FROM openjdk:17-alpine
MAINTAINER isKONSTANTIN <me@knst.su>

EXPOSE 8080

WORKDIR /finwave

COPY ./build/libs/FinTrack.jar ./

ENTRYPOINT ["java", "-jar", "FinTrack.jar"]