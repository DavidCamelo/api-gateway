FROM maven:3-eclipse-temurin-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean install -DskipTests=true --no-transfer-progress

FROM eclipse-temurin:21-jammy
COPY --from=build /home/app/target/*.jar /usr/local/lib/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /usr/local/lib/app.jar" ]