# 1. Сборка
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

# 2. Запуск
FROM eclipse-temurin:17-jre
EXPOSE 8095
RUN mkdir /app
# ВАЖНО: копируем только файл, заканчивающийся на -all.jar
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/ktor-app.jar
ENTRYPOINT ["java","-jar","/app/ktor-app.jar"]