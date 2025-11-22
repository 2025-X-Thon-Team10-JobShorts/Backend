# ShortForm Domain

숏폼 비디오 관리 및 AI 기반 컨텐츠 처리를 담당하는 도메인입니다.

## 개요

ShortForm 도메인은 사용자가 업로드한 숏폼 비디오의 메타데이터를 관리하고, AWS S3를 통한 파일 업로드, AI 기반 음성 인식(STT) 및 요약 기능을 제공합니다.

## 주요 기능

### 1. 비디오 업로드
- AWS S3 Pre-signed URL을 통한 안전한 파일 업로드
- 동영상 파일 키 생성 및 관리
- 미디어 타입 검증

### 2. 숏폼 메타데이터 관리
- 제목, 설명, 소유자 정보 관리
- 비디오 재생 시간 추적
- 가시성 설정 (공개/비공개/미등록)
- 상태 관리 (업로드 중, 준비완료, STT 처리 중 등)

### 3. AI 기반 컨텐츠 처리
- 음성 인식(STT)을 통한 자막 생성
- AI 기반 비디오 요약
- 처리 상태 및 오류 관리

## 아키텍처 구조

```
shortform/
├── controller/          # REST API 엔드포인트
├── dto/                 # 데이터 전송 객체
│   ├── request/        # 요청 DTO
│   └── response/       # 응답 DTO
├── entity/             # JPA 엔티티
├── repository/         # 데이터 액세스 계층
└── service/            # 비즈니스 로직
```

## 주요 엔티티

### ShortForm
숏폼 비디오의 메인 엔티티입니다.

**주요 필드:**
- `id`: 고유 식별자
- `ownerPid`: 소유자 ID
- `title`: 비디오 제목
- `description`: 비디오 설명
- `videoKey`: S3 비디오 파일 키
- `thumbnailKey`: S3 썸네일 파일 키
- `durationSec`: 재생 시간(초)
- `status`: 처리 상태 (ShortFormStatus)
- `visibility`: 가시성 설정 (VisibilityType)

### ShortFormAi
AI 처리 관련 정보를 저장하는 엔티티입니다.

**주요 필드:**
- `shortFormId`: 연관된 숏폼 ID
- `sttProvider`: STT 서비스 제공자
- `transcript`: 음성 인식 결과
- `summary`: AI 생성 요약
- `status`: AI 처리 상태 (ShortFormAiStatus)
- `errorMessage`: 오류 메시지

## 상태 관리

### ShortFormStatus
- `UPLOADING`: 업로드 중
- `READY`: 업로드 완료, 처리 준비
- `PROCESSING_STT`: STT 처리 중
- `READY_WITH_AI`: AI 처리 완료
- `FAILED`: 처리 실패

### VisibilityType
- `PUBLIC`: 공개
- `PRIVATE`: 비공개
- `UNLISTED`: 미등록 (링크로만 접근 가능)

### ShortFormAiStatus
- `PENDING`: 처리 대기
- `PROCESSING`: 처리 중
- `DONE`: 처리 완료
- `FAILED`: 처리 실패

## API 엔드포인트

### POST /short-forms/upload-url
비디오 업로드를 위한 Pre-signed URL을 생성합니다.

**요청:**
```json
{
  "fileName": "video.mp4",
  "mimeType": "video/mp4"
}
```

**응답:**
```json
{
  "uploadUrl": "https://s3.amazonaws.com/...",
  "videoKey": "videos/uuid/video.mp4"
}
```

### POST /short-forms
새로운 숏폼을 생성합니다.

**요청:**
```json
{
  "ownerPid": "user123",
  "title": "내 첫 숏폼",
  "description": "숏폼 설명",
  "videoKey": "videos/uuid/video.mp4",
  "durationSec": 30
}
```

### GET /short-forms/{id}
특정 숏폼의 상세 정보를 조회합니다.

**응답:**
```json
{
  "id": 1,
  "title": "내 첫 숏폼",
  "description": "숏폼 설명",
  "videoUrl": "https://s3.amazonaws.com/...",
  "thumbnailUrl": "https://s3.amazonaws.com/...",
  "durationSec": 30,
  "status": "READY_WITH_AI",
  "visibility": "PUBLIC",
  "summary": "AI 생성 요약 내용",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### 1) 숏폼 상세 (릴스 뷰)

**Endpoint**

- `GET /api/short-forms/{id}`

**Response**

```json
{
  "id": 1,
  "owner": {
    "id": 10,
    "displayName": "DongMin",
    "profileImageUrl": "...",
    "isFollowed": true
  },
  "videoUrl": "https://cdn.xxx.com/videos/uuid.mp4",
  "title": "...",
  "description": "...",
  "durationSec": 32,
  "summary": "요약된 자기소개 (AI)",
  "tags": ["frontend", "react"],
  "createdAt": "...",
  "aiStatus": "DONE"
}
```

### 2) 숏폼 피드 (무한 스크롤) - TanStack Query Infinity

**Endpoint**

- `GET /api/short-forms/feed?pageParam=...&size=10`

**Query Parameters:**
- `pageParam`: 다음 페이지를 위한 커서 (첫 페이지에서는 생략)
- `size`: 페이지당 아이템 수 (기본값: 10)

**Response**

```json
{
  "data": [
    {
      "id": 1,
      "owner": {
        "id": 10,
        "displayName": "DongMin",
        "profileImageUrl": "...",
        "isFollowed": true
      },
      "videoUrl": "https://cdn.xxx.com/videos/uuid.mp4",
      "title": "...",
      "description": "...",
      "durationSec": 32,
      "summary": "요약된 자기소개 (AI)",
      "tags": ["frontend", "react"],
      "createdAt": "...",
      "aiStatus": "DONE"
    }
  ],
  "nextPageParam": "eyJpZCI6MSwi...",
  "hasNextPage": true
}
```

**TanStack Query Infinity 스펙:**
- `data`: 현재 페이지의 숏폼 리스트
- `nextPageParam`: 다음 페이지 요청에 사용할 커서 (마지막 페이지면 null)
- `hasNextPage`: 다음 페이지 존재 여부

## 서비스 의존성

- **AwsS3Service**: AWS S3 관련 작업 (업로드 URL 생성, 파일 관리)
- **ShortFormRepository**: 숏폼 데이터 액세스
- **ShortFormAiRepository**: AI 처리 데이터 액세스

## 비즈니스 플로우

1. **업로드 URL 요청** → S3 Pre-signed URL 생성
2. **클라이언트 파일 업로드** → S3에 직접 업로드
3. **숏폼 생성 요청** → 메타데이터 저장 (READY 상태)
4. **썸네일 생성 시작** → FFmpeg 기반 비동기 처리
5. **AI 처리 시작** → STT 및 요약 처리 (PROCESSING_STT 상태)
6. **처리 완료** → 썸네일 및 AI 결과 저장 (READY_WITH_AI 상태)

## 썸네일 생성 시스템

### FFmpeg 기반 실제 구현
- **라이브러리**: JavaCV + FFmpeg 5.1.2
- **처리 방식**: S3 다운로드 → 프레임 추출 → 리사이징 → S3 업로드
- **썸네일 규격**: 320x180 JPEG 고품질
- **프레임 추출**: 비디오 1초 지점 (또는 전체 길이의 10%)

### 핵심 컴포넌트
- `ThumbnailGeneratorService`: 전용 썸네일 생성 서비스
- **비동기 처리**: `@EnableAsync` 기반 백그라운드 실행
- **재시도 로직**: 최대 3회 재시도, 2초 간격
- **오류 처리**: 각 단계별 상세 로깅 및 예외 처리

### 처리 과정
```
1. S3에서 비디오 파일 다운로드
2. FFmpeg로 1초 지점 프레임 추출  
3. 320x180 고품질 리사이징
4. JPEG 변환 후 S3 업로드
5. DB에 thumbnailKey 업데이트
6. 임시 파일 자동 정리
```

## AI 처리 시스템

### 비동기 STT 및 요약
- **상태 관리**: ShortFormAi 엔티티로 처리 상태 추적
- **처리 단계**: 음성 추출 → STT API 호출 → AI 요약 생성
- **결과 저장**: transcript, summary, extraJson 필드에 저장

## 사용자 연동 시스템

### 실제 User/Follow 도메인 연동
- **사용자 정보**: User 엔티티에서 실제 프로필 정보 조회
- **팔로우 상태**: Follow 엔티티로 실시간 팔로우 관계 확인
- **인증**: X-User-Pid 헤더를 통한 현재 사용자 식별

## 커서 기반 페이지네이션

### TanStack Query Infinity 완벽 지원
- **커서 인코딩**: Base64 인코딩으로 안전한 페이지 파라미터
- **응답 구조**: `data`, `nextPageParam`, `hasNextPage`
- **성능 최적화**: 필요한 만큼만 조회하는 효율적 쿼리

## 의존성

### 새로 추가된 라이브러리
```gradle
// FFmpeg 썸네일 생성
implementation 'org.bytedeco:javacv-platform:1.5.8'
implementation 'org.bytedeco:ffmpeg-platform:5.1.2-1.5.8'
```

### 연동 도메인
- **User Domain**: 사용자 정보 및 프로필
- **Follow Domain**: 팔로우 관계 관리
- **AWS S3**: 파일 저장 및 CDN

## 주의사항

- 모든 파일 업로드는 AWS S3 Pre-signed URL을 통해 수행됩니다
- 썸네일 생성은 FFmpeg를 사용한 실제 비디오 처리로 수행됩니다
- AI 처리는 비동기로 수행되며, 상태 확인을 통해 진행상황을 추적할 수 있습니다
- 재시도 로직으로 안정성을 보장하며, 실패 시 상세 로그를 기록합니다
- 팔로우 상태는 현재 사용자 기준으로 실시간 계산됩니다
- 임시 파일은 처리 완료 후 자동으로 정리됩니다


