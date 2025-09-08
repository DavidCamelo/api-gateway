FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /home/app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress
RUN rm -rf /root/.m2/repository

FROM eclipse-temurin:21-jre-jammy AS final
COPY --from=build /home/app/target/*.jar /usr/local/lib/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /usr/local/lib/app.jar" ]