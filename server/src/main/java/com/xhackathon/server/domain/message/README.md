# Message Domain - 실시간 메시징 시스템

메시지 도메인은 사용자 간의 1:1 DM(Direct Message) 기능을 제공합니다. REST API와 WebSocket을 통한 실시간 통신을 지원합니다.

## 주요 기능
- **스레드 관리**: 사용자 간 대화 스레드 생성 및 조회
- **메시지 전송**: 실시간 메시지 전송 및 내역 조회
- **읽음 처리**: 메시지 읽음 상태 관리 및 실시간 알림
- **WebSocket 실시간 통신**: 새 메시지 및 읽음 알림 실시간 전송
- **PostgreSQL 연동**: 메시지 및 스레드 데이터 영구 저장

## 기술 스택
- `Spring Boot 3.3.2`: 백엔드 프레임워크
- `Spring Data JPA`: 데이터 액세스 레이어
- `PostgreSQL`: 메시지 데이터베이스
- `WebSocket`: 실시간 통신
- `OpenAPI 3.0`: API 문서화
- `Lombok`: 코드 간소화

## 도메인 구조
```
domain/message/
├── controller/
│   └── MessageController.java          # REST API 엔드포인트
├── service/
│   └── MessageService.java             # 비즈니스 로직
├── repository/
│   ├── MessageRepository.java          # 메시지 데이터 액세스
│   └── ThreadRepository.java           # 스레드 데이터 액세스
├── entity/
│   ├── Message.java                    # 메시지 엔티티
│   └── Thread.java                     # 스레드 엔티티
├── dto/
│   ├── request/
│   │   ├── CreateThreadRequest.java
│   │   ├── SendMessageRequest.java
│   │   └── ThreadListRequest.java
│   └── response/
│       ├── MessageResponse.java
│       ├── ThreadResponse.java
│       └── ThreadMessagesResponse.java
└── websocket/
    ├── MessageWebSocketHandler.java    # WebSocket 메시지 핸들러
    ├── WebSocketMessage.java           # WebSocket 메시지 DTO
    ├── WebSocketSessionManager.java    # 세션 관리
    └── WebSocketNotificationService.java # 실시간 알림 서비스
```

## 사전 요구사항
- **Java 21** 이상
- **PostgreSQL 14** 실행 중
- 데이터베이스 `xhackathon` 생성

## 실행 방법

### 1. 데이터베이스 준비

**macOS/Linux:**
```bash
createdb xhackathon
```

**Windows:**
```sql
-- PostgreSQL에 접속하여 실행
CREATE DATABASE xhackathon;
```

### 2. 애플리케이션 실행

**macOS/Linux:**
```bash
cd server
./gradlew clean build
./gradlew bootRun
```

**Windows (PowerShell 또는 CMD):**
```cmd
cd server
gradlew.bat clean build
gradlew.bat bootRun
```

**주의사항:**
- Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용해야 합니다
- Java 21 이상이 설치되어 있어야 합니다 (`java -version`으로 확인)
- PostgreSQL이 실행 중이어야 합니다

### 3. API 확인
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **헬스 체크**: http://localhost:8080/api/health
- **API 문서**: http://localhost:8080/v3/api-docs

### 4. WebSocket 연결
- **WebSocket URL**: `ws://localhost:8080/ws/messages`

## API 엔드포인트

### REST API

#### 스레드 관리
- `GET /api/messages/threads` - 사용자의 스레드 목록 조회
- `POST /api/messages/threads` - 새 스레드 생성 (기존 스레드가 있으면 반환)

#### 메시지 관리  
- `GET /api/messages/threads/{threadId}` - 특정 스레드의 메시지 내역 조회 및 읽음 처리
- `POST /api/messages/threads/{threadId}/messages` - 메시지 전송

### WebSocket API

#### 연결 및 초기화
```javascript
// 1. WebSocket 연결
const ws = new WebSocket('ws://localhost:8080/ws/messages');

// 2. 사용자 초기화
ws.send(JSON.stringify({
    type: 'INIT',
    currentUserPid: 'user_123'
}));

// 3. 특정 스레드 구독
ws.send(JSON.stringify({
    type: 'SUBSCRIBE',
    threadId: 456
}));
```

#### 실시간 알림
- **MESSAGE_CREATED**: 새 메시지 수신 시
- **MESSAGE_READ**: 상대방이 메시지를 읽었을 때

## 엔티티 관계

### Thread 엔티티
- `id`: 스레드 고유 ID (PK)
- `participant1Pid`, `participant2Pid`: 참여자 사용자 ID
- `createdAt`, `updatedAt`: 생성/수정 시각

### Message 엔티티
- `id`: 메시지 고유 ID (PK)
- `threadId`: 소속 스레드 ID (FK)
- `senderPid`: 발신자 사용자 ID
- `content`: 메시지 내용
- `createdAt`: 생성 시각
- `readAt`: 읽음 시각 (null이면 미읽음)

## 개발 가이드
- 도메인별 패키지 구조 유지
- 새로운 도메인은 `/domain/` 하위에 추가
- DTO는 요청/응답별로 분리하여 관리
- OpenAPI 어노테이션으로 API 문서화 필수

## 라이선스
MIT 또는 프로젝트 요구 사항에 맞는 라이선스를 `LICENSE` 파일로 추가해주세요.
