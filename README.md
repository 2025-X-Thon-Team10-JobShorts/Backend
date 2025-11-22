# X Hackathon - API Server

API를 제공하는 Spring Boot 서버입니다. `Spring Boot 3.3`, `Java 21`을 기반으로 실시간 메시징 기능을 제공합니다.

## 주요 기능
- **DM API**: 스레드 생성, 메시지 전송, 메시지 내역 조회
- **실시간 WebSocket**: 실시간 메시지 알림
- **PostgreSQL 연동**: 메시지 데이터 영구 저장
- **OpenAPI 문서**: Swagger UI를 통한 API 문서화
- **도메인 기반 아키텍처**: 관심사 분리된 패키지 구조

## 기술 스택
- `Spring Boot 3.3.2`: 백엔드 프레임워크
- `Spring Data JPA`: 데이터 액세스 레이어
- `PostgreSQL`: 메시지 데이터베이스
- `WebSocket`: 실시간 통신
- `OpenAPI 3.0`: API 문서화
- `Lombok`: 코드 간소화

## 디렉터리 구조
```
server/
├── build.gradle
├── src/main/java/com/xhackathon/server/
│   ├── ServerApplication.java
│   ├── DataInitializer.java
│   ├── domain/
│   │   ├── message/                    # 메시지 도메인
│   │   │   ├── controller/             # REST API 컨트롤러
│   │   │   ├── service/                # 비즈니스 로직
│   │   │   ├── repository/             # 데이터 액세스
│   │   │   ├── entity/                 # JPA 엔티티
│   │   │   ├── dto/                    # 데이터 전송 객체
│   │   │   │   ├── request/            # 요청 DTO
│   │   │   │   └── response/           # 응답 DTO
│   │   │   └── websocket/              # WebSocket 핸들러
│   │   └── user/                       # 사용자 도메인
│   │       ├── entity/
│   │       ├── repository/
│   │       └── dto/
│   └── common/                         # 공통 컴포넌트
│       ├── config/                     # 설정 클래스
│       ├── controller/                 # 공통 컨트롤러
│       └── dto/                        # 공통 DTO
└── src/main/resources/
    └── application.properties          # 데이터베이스 설정
```

## 사전 요구사항
- **Java 21** 이상
- **PostgreSQL 14** 실행 중
- 데이터베이스 `xhackathon` 생성

## 실행 방법

### 1. 데이터베이스 준비
```bash
# PostgreSQL에서 데이터베이스 생성
createdb xhackathon
```

### 2. 애플리케이션 실행
```bash
cd server
./gradlew clean build
./gradlew bootRun
```

### 3. API 확인
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **헬스 체크**: http://localhost:8080/api/health
- **API 문서**: http://localhost:8080/v3/api-docs

### 4. WebSocket 연결
- **WebSocket URL**: `ws://localhost:8080/ws`

## API 엔드포인트

### 스레드 관리
- `GET /api/messages/threads` - 스레드 목록 조회
- `POST /api/messages/threads` - 새 스레드 생성

### 메시지 관리  
- `GET /api/messages/threads/{threadId}` - 메시지 내역 조회
- `POST /api/messages/threads/{threadId}/messages` - 메시지 전송

### 실시간 기능
- WebSocket을 통한 실시간 메시지 알림
- 메시지 읽음 상태 실시간 업데이트

## 개발 가이드
- 도메인별 패키지 구조 유지
- 새로운 도메인은 `/domain/` 하위에 추가
- DTO는 요청/응답별로 분리하여 관리
- OpenAPI 어노테이션으로 API 문서화 필수

## 라이선스
MIT 또는 프로젝트 요구 사항에 맞는 라이선스를 `LICENSE` 파일로 추가해주세요.
