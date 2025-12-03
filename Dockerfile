###
# Image pour la compilation
FROM maven:3-eclipse-temurin-21 AS build-image
WORKDIR /build/
# Installation et configuration de la locale FR
RUN apt update && DEBIAN_FRONTEND=noninteractive apt -y install locales



# On lance la compilation Java
# On débute par une mise en cache docker des dépendances Java
# cf https://www.baeldung.com/ops/docker-cache-maven-dependencies
COPY ./pom.xml /build/logskbart-api/pom.xml
RUN mvn verify --fail-never
# et la compilation du code Java
COPY ./   /build/

RUN mvn --batch-mode \
        -Dmaven.test.skip=true \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        package spring-boot:repackage


###
# Image pour le module API
FROM ossyupiik/java:21.0.8 AS logskbart-api-image
WORKDIR /
COPY --from=build-image /build/target/*.jar /app/logskbart-api.jar

ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD ["java", "-jar", "/app/logskbart-api.jar"]
