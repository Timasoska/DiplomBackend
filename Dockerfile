# 1. Сборка
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

# 2. Запуск
FROM eclipse-temurin:17-jre
EXPOSE 5555
# Создаем рабочую директорию
WORKDIR /app
# Копируем jar внутрь
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/ktor-app.jar
# Запускаем
ENTRYPOINT ["java","-jar","ktor-app.jar"]