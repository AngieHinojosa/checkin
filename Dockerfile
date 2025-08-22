# Etapa build: compila el JAR
FROM maven:3.9.10-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa runtime: JRE liviano
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/bsale-checkin-0.0.1.jar app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java","-jar","app.jar"]
