# ── 1단계: 빌드 ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon -q

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test -q

# ── 2단계: 런타임 ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# HEIC→JPEG 서버 변환용 네이티브 바이너리 (heif-convert = libheif-tools, magick = imagemagick)
RUN apk add --no-cache libheif-tools imagemagick

RUN addgroup -S haemi && adduser -S haemi -G haemi
USER haemi

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=60.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
