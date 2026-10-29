# deploy.yml이 ./gradlew bootJar로 만든 jar를 담는다. 빌드는 이미지 밖(Actions)에서 한다.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar

# GCP e2-micro(1GB)에서 Postgres·nginx와 같이 돌리기 위한 힙 제한
ENV JAVA_OPTS="-Xms256m -Xmx256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
