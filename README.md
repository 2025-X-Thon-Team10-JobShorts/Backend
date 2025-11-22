# X Hackathon - API Server

API를 제공하는 Spring Boot 서버입니다. `Spring Boot 3.3`, `Java 21`을 기반으로 사용자 인증, 팔로우 시스템, 마이페이지 기능을 제공합니다.

## 주요 기능
- **Auth API**: 사용자 회원가입, 로그인
- **Follow API**: 팔로우/언팔로우, 팔로워/팔로잉 목록 조회
- **MyPage API**: 사용자 정보 조회 및 수정
- **User API**: 사용자 데이터 관리
- **PostgreSQL 연동**: 사용자 데이터 영구 저장
- **OpenAPI 문서**: Swagger UI를 통한 API 문서화
- **도메인 기반 아키텍처**: 관심사 분리된 패키지 구조

## 기술 스택
- `Spring Boot 3.3.2`: 백엔드 프레임워크
- `Spring Data JPA`: 데이터 액세스 레이어
- `PostgreSQL`: 사용자 데이터베이스
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

## 개발 가이드
- 도메인별 패키지 구조 유지
- 새로운 도메인은 `/domain/` 하위에 추가
- DTO는 요청/응답별로 분리하여 관리
- OpenAPI 어노테이션으로 API 문서화 필수

## 라이선스
MIT 또는 프로젝트 요구 사항에 맞는 라이선스를 `LICENSE` 파일로 추가해주세요.
