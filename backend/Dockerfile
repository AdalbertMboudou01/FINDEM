FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY ./gradlew .
COPY ./gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x ./gradlew
COPY src ./src
RUN ./gradlew build -x test
EXPOSE 8000
CMD ["java", "-jar", "build/libs/assistant-backend-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=docker"]
