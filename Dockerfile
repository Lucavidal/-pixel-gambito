# Imagen para publicar la versión web (Railway, Fly.io, Render o cualquier servidor con Docker).
# Etapa 1: compila el jar (sin tests, ya se corren en la PC con "mvn test").
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# Etapa 2: solo el JRE y el jar.
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/tpo-chess-1.0.0.jar /app/app.jar
# ajedrez.db se crea en la carpeta de trabajo: montar un volumen persistente en /data
# para no perder cuentas e historial en cada deploy.
WORKDIR /data
VOLUME /data
EXPOSE 8080
# Las plataformas pasan el puerto en $PORT; si no hay, 8080.
# CHESS_SECURE_COOKIE=true cuando el sitio se sirve por HTTPS (Railway, Fly y Render lo hacen).
ENV CHESS_SECURE_COOKIE=false
CMD ["sh", "-c", "exec java -Xmx256m -Djava.awt.headless=true -Dchess.port=${PORT:-8080} -Dchess.secureCookie=${CHESS_SECURE_COOKIE} -jar /app/app.jar web"]
