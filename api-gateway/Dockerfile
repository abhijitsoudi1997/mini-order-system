# ---- Stage 1: build the jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B clean package -DskipTests

# ---- Stage 2: the small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
