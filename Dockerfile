FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x ./mvnw && \
    ./mvnw clean install

EXPOSE 8080

CMD ["./mvnw", "spring-boot:run"]