FROM openjdk:17-alpine
MAINTAINER isKONSTANTIN <me@knst.su>

EXPOSE 8080

WORKDIR /finwave

COPY ./FinWave.jar ./

ENTRYPOINT ["java", "-jar", "FinWave.jar"]