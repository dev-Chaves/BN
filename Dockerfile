# Stage 1: Build (Focado em velocidade de build)
FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app

# 1. Cache de dependências: Copia o pom e baixa as libs antes do código
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Compilação: Copia o src e gera o pacote Quarkus (fast-jar)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run (Focado em economia de RAM para o WSL/Produção)
FROM amazoncorretto:21-alpine
WORKDIR /app

# 3. Segurança: Criar usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 4. Copia artefatos Quarkus (fast-jar)
COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

EXPOSE 8080

# 5. Flags de enxugamento da JVM:
# -XX:+UseSerialGC: GC mais leve para apps pequenos
# -Xss512k: Reduz custo de memória por Thread
# -Xmx384m: Limite de 384MB de Heap
# -XX:MaxMetaspaceSize=72m: Evita que o uso de classes cresça indefinidamente
ENTRYPOINT ["java", \
            "-XX:+UseSerialGC", \
            "-Xss512k", \
            "-Xmx384m", \
            "-XX:MaxMetaspaceSize=72m", \
            "-XX:+UseContainerSupport", \
            "-jar", "/app/quarkus-run.jar"]
