FROM openjdk:11
ARG JAR_FILE=build/libs/simple-hello*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]