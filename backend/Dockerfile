FROM eclipse-temurin:25-jdk

WORKDIR /workspace
COPY . .
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/* && chmod +x mvnw

EXPOSE 8080

ENTRYPOINT ["./mvnw", "-B", "spring-boot:run"]
