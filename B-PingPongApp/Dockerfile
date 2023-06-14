FROM openjdk:11
LABEL authors="Pedro Akos Costa"

WORKDIR /app

COPY target/B-PingPongApp.jar /app/B-PingPongApp.jar
COPY log4j2.xml /app/log4j2.xml

ENTRYPOINT ["java", "-Dlog4j2.configurationFile=log4j2.xml", "-jar", "B-PingPongApp.jar"]
