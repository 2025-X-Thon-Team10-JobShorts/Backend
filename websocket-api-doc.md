# WebSocket API 문서

## 개요

DM API는 실시간 메시징을 위해 WebSocket을 지원합니다. WebSocket을 통해 메시지 전송/수신과 실시간 알림을 받을 수 있습니다.

## WebSocket 엔드포인트

```
ws://localhost:8080/ws/messages
```

## 연결 및 인증

### 1. WebSocket 연결
클라이언트는 WebSocket 연결 후 첫 번째 메시지로 사용자 인증을 수행해야 합니다.

```json
{
  "type": "INIT",
  "currentUserPid": "user_me"
}
```

### 2. 스레드 구독
특정 스레드의 실시간 메시지를 수신하려면 해당 스레드를 구독해야 합니다.

```json
{
  "type": "SUBSCRIBE",
  "threadId": 12
}
```

## 서버에서 클라이언트로 전송되는 메시지

### 1. 새 메시지 알림
다른 사용자가 메시지를 전송했을 때 실시간으로 수신됩니다.

```json
{
  "type": "MESSAGE_CREATED",
  "threadId": 12,
  "message": {
    "id": 110,
    "senderPid": "user_21",
    "content": "안녕하세요 관심 있습니다!",
    "createdAt": "2025-11-21T12:40:00Z",
    "readAt": null
  }
}
```

### 2. 메시지 읽음 알림
상대방이 메시지를 읽었을 때 전송됩니다.

```json
{
  "type": "MESSAGE_READ",
  "threadId": 12,
  "messageId": 110,
  "readerPid": "user_21",
  "readAt": "2025-11-21T12:41:00Z"
}
```

## 메시지 타입 정의

| 타입 | 방향 | 설명 |
|------|------|------|
| `INIT` | Client → Server | 사용자 인증 및 초기화 |
| `SUBSCRIBE` | Client → Server | 특정 스레드 구독 |
| `MESSAGE_CREATED` | Server → Client | 새 메시지 알림 |
| `MESSAGE_READ` | Server → Client | 메시지 읽음 알림 |

## 예제 코드

### JavaScript 클라이언트

```javascript
// WebSocket 연결
const ws = new WebSocket('ws://localhost:8080/ws/messages');

// 연결 시 초기화
ws.onopen = function() {
    ws.send(JSON.stringify({
        type: 'INIT',
        currentUserPid: 'user_me'
    }));
};

// 스레드 구독
function subscribeToThread(threadId) {
    ws.send(JSON.stringify({
        type: 'SUBSCRIBE',
        threadId: threadId
    }));
}

// 메시지 수신
ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    
    switch(message.type) {
        case 'MESSAGE_CREATED':
            console.log('새 메시지:', message.message);
            break;
        case 'MESSAGE_READ':
            console.log('메시지 읽음:', message);
            break;
    }
};
```

## 주의사항

1. **연결 유지**: WebSocket 연결이 끊어지면 자동으로 재연결 로직을 구현하는 것을 권장합니다.
2. **구독 관리**: 페이지를 떠날 때나 필요 없는 스레드는 구독을 해제하여 불필요한 메시지 수신을 방지합니다.
3. **메시지 순서**: 네트워크 상황에 따라 메시지 순서가 바뀔 수 있으므로 `createdAt` 필드를 기준으로 정렬하세요.