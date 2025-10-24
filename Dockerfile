FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests

# Étape 2 : image finale légère
FROM eclipse-temurin:21-jre-alpine

# Créer un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copier le JAR depuis l'étape précédente
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port de l'application
EXPOSE 8081

# Lancer l'application
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app.jar"]
