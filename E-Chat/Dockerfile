FROM openjdk:11
LABEL authors="Pedro Akos Costa"

WORKDIR /app

COPY target/E-Chat.jar /app/E-Chat.jar
COPY log4j2.xml /app/log4j2.xml

ENTRYPOINT ["java", "-Dlog4j2.configurationFile=log4j2.xml", "-jar", "E-Chat.jar"]
