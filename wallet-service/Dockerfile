# Multi-stage Dockerfile pour Wallet Service
# Stage 1: Build avec Maven
FROM maven:3.9.4-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml ../pom.xml
COPY wallet-service/pom.xml ./pom.xml

# Télécharger les dépendances
RUN mvn dependency:go-offline -B

# Copier le code source
COPY wallet-service/src ./src

# Build de l'application
RUN mvn clean package -DskipTests

# Stage 2: Runtime avec JRE minimal
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Installer wget pour les health checks
RUN apk add --no-cache wget

# Créer un utilisateur non-root
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring
USER spring:spring

# Copier le JAR depuis le stage de build
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Commande de démarrage avec profil Docker
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "/app/app.jar"]
