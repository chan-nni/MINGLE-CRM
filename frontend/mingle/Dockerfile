# 기본 이미지로부터 시작
FROM node:20.12.1

# 작업 디렉토리 생성
WORKDIR /app

# 호스트의 소스 코드를 컨테이너의 작업 디렉토리로 복사
COPY . .

# 프로덕션 환경을 위한 소스 코드 빌드
RUN npm install
RUN npm run build

# Nginx 이미지를 사용하여 정적 파일 서버 설정
FROM nginx:alpine
COPY --from=0 /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
