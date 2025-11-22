# X Hackathon - API Server

API를 제공하는 Spring Boot 서버입니다. `Spring Boot 3.3`, `Java 21`을 기반으로 사용자 인증, 팔로우 시스템, 마이페이지 기능을 제공합니다.

## 주요 기능
- **Auth API**: 사용자 회원가입, 로그인
- **Follow API**: 팔로우/언팔로우, 팔로워/팔로잉 목록 조회
- **MyPage API**: 사용자 정보 조회 및 수정
- **Message API**: 1:1 DM 기능, 스레드 관리, 실시간 메시징
- **ShortForm API**: 쇼트폼 영상 업로드, 조회, AI 처리 (STT, 요약)
- **WebSocket**: 실시간 메시지 및 알림 통신
- **PostgreSQL 연동**: 사용자 데이터 영구 저장
- **AWS S3 연동**: Presigned URL을 통한 영상 파일 저장 및 요약 파일 관리
- **OpenAPI 문서**: Swagger UI를 통한 API 문서화
- **도메인 기반 아키텍처**: 관심사 분리된 패키지 구조

## 기술 스택
- `Spring Boot 3.3.2`: 백엔드 프레임워크
- `Spring Data JPA`: 데이터 액세스 레이어
- `PostgreSQL`: 사용자 데이터베이스
- `WebSocket`: 실시간 통신
- `AWS S3`: 영상 파일 저장
- `OpenAPI 3.0`: API 문서화
- `Lombok`: 코드 간소화

## 디렉터리 구조
```
server/
├── build.gradle
├── src/main/java/com/xhackathon/server/
│   ├── ServerApplication.java
│   ├── domain/
│   │   ├── autho/                      # 인증 도메인
│   │   │   ├── controller/             # 회원가입, 로그인 API
│   │   │   ├── service/                # 인증 비즈니스 로직
│   │   │   └── dto/                    # 인증 DTO
│   │   │       ├── request/            # 요청 DTO
│   │   │       └── response/           # 응답 DTO
│   │   ├── follow/                     # 팔로우 도메인
│   │   │   ├── controller/             # 팔로우 API
│   │   │   ├── service/                # 팔로우 비즈니스 로직
│   │   │   ├── repository/             # 데이터 액세스
│   │   │   ├── entity/                 # JPA 엔티티
│   │   │   └── dto/                    # 팔로우 DTO
│   │   │       ├── request/            # 요청 DTO
│   │   │       └── response/           # 응답 DTO
│   │   ├── mypage/                     # 마이페이지 도메인
│   │   │   ├── controller/             # 마이페이지 API
│   │   │   ├── service/                # 마이페이지 비즈니스 로직
│   │   │   └── dto/                    # 마이페이지 DTO
│   │   │       ├── request/            # 요청 DTO
│   │   │       └── response/           # 응답 DTO
│   │   ├── message/                    # 메시지 도메인
│   │   │   ├── controller/             # 메시지 API
│   │   │   ├── service/                # 메시지 비즈니스 로직
│   │   │   ├── repository/             # 메시지 데이터 액세스
│   │   │   ├── entity/                 # 메시지 엔티티
│   │   │   ├── dto/                    # 메시지 DTO
│   │   │   └── websocket/              # WebSocket 핸들러
│   │   ├── shortform/                  # 쇼트폼 도메인
│   │   │   ├── controller/             # 쇼트폼 API
│   │   │   ├── service/                # 쇼트폼 비즈니스 로직 (AwsS3Service 포함)
│   │   │   ├── repository/             # 쇼트폼 데이터 액세스
│   │   │   ├── entity/                 # 쇼트폼 엔티티 (ShortForm, ShortFormAi)
│   │   │   └── dto/                    # 쇼트폼 DTO
│   │   └── user/                       # 사용자 도메인
│   │       ├── entity/                 # 사용자 엔티티
│   │       ├── repository/             # 사용자 데이터 액세스
│   │       └── dto/                    # 사용자 DTO
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
- **AWS S3 버킷** 설정 및 접근 권한 구성
- AWS 자격 증명 설정 (`~/.aws/credentials` 또는 환경 변수)

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
- **WebSocket URL**: ws://localhost:8080/ws/messages

## API 엔드포인트

### 인증 관리
- `POST /auth/signup` - 사용자 회원가입
- `POST /auth/login` - 사용자 로그인

### 팔로우 관리
- `POST /follow` - 팔로우/언팔로우 토글
- `GET /followings` - 팔로잉 목록 조회
- `GET /followers` - 팔로워 목록 조회

### 마이페이지 관리
- `POST /mypage/info` - 사용자 정보 조회
- `PATCH /mypage/update` - 사용자 정보 수정

### 메시지 관리
- `GET /api/messages/threads` - 사용자의 스레드 목록 조회
- `POST /api/messages/threads` - 새 스레드 생성
- `GET /api/messages/threads/{threadId}` - 특정 스레드의 메시지 내역 조회 및 읽음 처리
- `POST /api/messages/threads/{threadId}/messages` - 메시지 전송
- `WebSocket /ws/messages` - 실시간 메시지 통신

### 쇼트폼 관리

#### 주요 기능
- **Presigned URL 생성**: 클라이언트에서 직접 S3에 영상 업로드
- **쇼트폼 생성**: 업로드된 영상 정보를 데이터베이스에 저장
- **상세 조회**: AI 요약 포함 쇼트폼 상세 정보 조회
- **AI 처리**: STT(음성 인식) 및 요약 자동 생성

#### API 엔드포인트
- `POST /short-forms/upload-url` - 영상 업로드용 Presigned URL 생성
  - 영상 파일명과 MIME 타입을 받아 S3 업로드 URL 생성
  - 10분 유효기간의 Presigned URL 반환
- `POST /short-forms` - 쇼트폼 생성
  - 업로드 완료된 영상 정보를 데이터베이스에 저장
  - 기본 상태: `READY`, 공개 범위: `PUBLIC`
- `GET /short-forms/{id}` - 쇼트폼 상세 조회
  - 영상 메타데이터 및 AI 생성 요약 정보 포함
  - S3에 저장된 요약 JSON 파일 자동 로드

#### 사용 흐름
1. 클라이언트에서 업로드할 영상 파일 정보로 Presigned URL 요청
2. 받은 URL로 클라이언트가 직접 S3에 영상 업로드
3. 업로드 완료 후 영상 메타데이터로 쇼트폼 생성 API 호출
4. 쇼트폼 상세 조회 시 AI 요약 정보 함께 반환

#### 상태 관리
- `UPLOADING`: 영상 업로드 중
- `READY`: 업로드 완료, 준비됨
- `PROCESSING_STT`: AI 음성 인식 처리 중
- `READY_WITH_AI`: AI 처리 완료
- `FAILED`: 처리 실패

#### 공개 범위
- `PUBLIC`: 공개
- `PRIVATE`: 비공개
- `UNLISTED`: 비공개 목록

## 개발 가이드
- 도메인별 패키지 구조 유지
- 새로운 도메인은 `/domain/` 하위에 추가
- DTO는 요청/응답별로 분리하여 관리
- OpenAPI 어노테이션으로 API 문서화 필수

## 라이선스
MIT 또는 프로젝트 요구 사항에 맞는 라이선스를 `LICENSE` 파일로 추가해주세요.
