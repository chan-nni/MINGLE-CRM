# 기본 이미지로부터 시작
FROM openjdk:17-jdk-slim

# 작업 디렉토리 생성
WORKDIR /app

# 호스트의 JAR 파일을 컨테이너의 작업 디렉토리로 복사
COPY C:/encore/MINGLE-CRM/backend/minglecrm/build/libs/minglecrm-0.0.1-SNAPSHOT.jar /app/my-spring-app.jar

# 컨테이너가 실행될 때 실행할 명령어 설정
CMD ["java", "-jar", "my-spring-app.jar"]
