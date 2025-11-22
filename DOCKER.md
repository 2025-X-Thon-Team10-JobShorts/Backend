# Docker 빌드 및 실행 가이드

## 사전 요구사항
- Docker 설치
- Docker Compose 설치
- Docker Hub 계정 (이미지 푸시용)

## Docker 이미지 빌드 및 푸시

### 1. Docker 이미지 빌드
```bash
docker build -t doongmino0204/back-app:latest .
```

### 2. Docker Hub에 푸시
```bash
# Docker Hub 로그인
docker login

# 이미지 푸시
docker push doongmino0204/back-app:latest
```

### 3. 로컬에서 이미지 테스트
```bash
docker pull doongmino0204/back-app:latest
docker-compose up
```

## Docker Compose로 실행

### 1. 환경 변수 설정 (선택사항)
`.env` 파일을 생성하여 AWS 설정을 추가할 수 있습니다:
```bash
CLOUD_AWS_S3_BUCKET=your-bucket-name
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=ap-northeast-2
```

### 2. Docker Compose 실행
```bash
# 백그라운드로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f backend

# 서비스 중지
docker-compose down

# 데이터베이스 데이터까지 삭제
docker-compose down -v
```

### 3. 서비스 확인
- **백엔드 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **PostgreSQL**: localhost:5432
  - 데이터베이스: `xhackathon`
  - 사용자: `xhackathon`
  - 비밀번호: `xhackathon_password`

## Docker Compose 서비스 구성

### PostgreSQL 14
- 포트: `5432`
- 볼륨: `postgres_data` (데이터 영구 저장)
- Health Check: 자동으로 데이터베이스 준비 상태 확인

### Backend (Spring Boot)
- 포트: `8080`
- 이미지: `doongmino0204/back-app:latest`
- PostgreSQL 서비스가 정상 상태가 된 후 자동 시작
- 환경 변수로 설정 가능

## 문제 해결

### 데이터베이스 연결 실패
```bash
# PostgreSQL 컨테이너 로그 확인
docker-compose logs postgres

# PostgreSQL 컨테이너에 직접 접속
docker-compose exec postgres psql -U xhackathon -d xhackathon
```

### 백엔드 애플리케이션 오류
```bash
# 백엔드 로그 확인
docker-compose logs -f backend

# 컨테이너 재시작
docker-compose restart backend
```

### 이미지 빌드 실패
```bash
# 캐시 없이 빌드
docker build --no-cache -t doongmino0204/back-app:latest .

# 중간 단계 확인
docker build -t doongmino0204/back-app:latest . --progress=plain
```

## 프로덕션 배포

프로덕션 환경에서는 다음을 고려하세요:

1. **환경 변수 관리**: `.env` 파일이나 Docker secrets 사용
2. **리소스 제한**: `docker-compose.yml`에 `deploy.resources` 추가
3. **헬스 체크**: 백엔드 서비스에도 health check 추가
4. **로깅**: 로그 드라이버 설정
5. **네트워크**: 보안을 위한 네트워크 분리

## 참고 사항

- PostgreSQL 데이터는 `postgres_data` 볼륨에 저장되므로 컨테이너를 삭제해도 데이터는 유지됩니다.
- AWS 자격 증명은 환경 변수로 전달되므로 `.env` 파일이나 Docker secrets를 사용하세요.
- 개발 환경에서는 `application.properties`의 설정이 우선 적용됩니다.

