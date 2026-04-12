# ThinkMap Backend

> **사고 흐름을 시각화하는 AI 학습 동반자** — 학생의 질문·탐구·이해 변화를 트리 구조로 저장하고 마인드맵으로 시각화합니다.

---

## 프로젝트 소개

ThinkMap은 단순한 AI 답변 서비스가 아닙니다.  
학생이 하나의 주제를 탐구하는 **학습 여정 전체**를 기록하고, 사고 흐름을 마인드맵 트리로 시각화하며, 세션 종료 후 AI가 6항목 **학습 회고 리포트**를 생성합니다.

| 핵심 가치 | 설명 |
|---|---|
| **자기주도학습** | AI가 답을 주지 않고 소크라테스식 질문으로 학생이 스스로 생각하도록 유도 |
| **사고 흐름 시각화** | 질문 → AI 답변 → 이해 수정의 흐름을 트리 구조로 저장·렌더링 |
| **학습 회고** | 세션 전체를 분석한 6항목 인사이트 리포트로 메타인지 강화 |

---

## 기술 스택

| 레이어 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.5 |
| ORM | Spring Data JPA (Hibernate) |
| DB (개발) | H2 In-Memory |
| DB (운영) | MySQL 8 (설정 준비됨) |
| AI | Google Gemini API (`gemini-2.5-flash`) |
| HTTP Client | Spring WebFlux WebClient (Reactor Netty) |
| API 문서 | SpringDoc OpenAPI 3 (Swagger UI) |
| 로깅 | Log4j2 |
| 빌드 | Gradle 8 |

---

## 시작하기

### 사전 요구 사항

- JDK 17+
- Gemini API Key ([Google AI Studio](https://aistudio.google.com/app/apikey)에서 발급)

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하세요 (`.env.example` 참고).

```bash
cp .env.example .env
```

`.env` 파일:

```properties
# Gemini API (필수)
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_BASE_URL=https://generativelanguage.googleapis.com
GEMINI_MODEL=gemini-2.5-flash

# MySQL (운영 전환 시 사용 — 개발 환경에서는 H2 자동 사용)
LOCAL_DB_URL=jdbc:mysql://localhost:3306/thinkmap_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
LOCAL_DB_USERNAME=root
LOCAL_DB_PASSWORD=your_password
```

### 실행

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080` 에서 시작됩니다.

| URL | 설명 |
|---|---|
| `http://localhost:8080/swagger-ui` | Swagger UI (API 탐색·테스트) |
| `http://localhost:8080/h2-console` | H2 콘솔 (개발용 DB 조회) |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON 스펙 |

> H2 콘솔 접속 정보: JDBC URL `jdbc:h2:mem:thinkmap`, 사용자명 `sa`, 비밀번호 없음

---

## API 레퍼런스

전체 API는 실행 후 `http://localhost:8080/swagger-ui` 에서 인터랙티브하게 확인할 수 있습니다.

### 학습 세션 (Sessions)

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/sessions` | 새 학습 세션 생성 |
| `GET` | `/api/sessions` | 전체 세션 목록 (최신 활동 순) |
| `GET` | `/api/sessions/{id}` | 세션 상세 조회 |
| `PATCH` | `/api/sessions/{id}` | 세션 제목 수정 |
| `DELETE` | `/api/sessions/{id}` | 세션 삭제 (하위 노드 포함 영구 삭제) |

#### 세션 생성 예시

```http
POST /api/sessions
Content-Type: application/json

{
  "title": "광합성은 어떻게 빛을 에너지로 바꾸는가"
}
```

```json
{
  "id": 1,
  "title": "광합성은 어떻게 빛을 에너지로 바꾸는가",
  "nodeCount": 0,
  "questionCount": 0,
  "hasInsight": false,
  "createdAt": "2026-04-09T13:00:00",
  "updatedAt": "2026-04-09T13:00:00"
}
```

---

### 학습 노드 & 마인드맵 (Nodes)

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/sessions/{sessionId}/nodes` | 질문·이해수정 노드 추가 + AI 답변 |
| `POST` | `/api/sessions/{sessionId}/nodes/insight` | 학습 회고 인사이트 생성 |
| `GET` | `/api/sessions/{sessionId}/nodes/tree` | 마인드맵 트리 조회 |

#### 질문 노드 추가 예시

```http
POST /api/sessions/1/nodes
Content-Type: application/json

{
  "content": "엽록체에서 실제로 빛을 흡수하는 분자는 무엇인가요?",
  "nodeType": "QUESTION",
  "parentNodeId": null
}
```

```json
{
  "questionNode": {
    "id": 1,
    "content": "엽록체에서 실제로 빛을 흡수하는 분자는 무엇인가요?",
    "summary": "엽록체 빛 흡수 분자",
    "nodeType": "QUESTION",
    "nodeTypeLabel": "질문",
    "depth": 0,
    "createdAt": "2026-04-09T13:01:00"
  },
  "answerNode": {
    "id": 2,
    "content": "좋은 질문입니다! 빛을 흡수하는 '주인공'을 찾고 계시군요. 그렇다면 엽록체 내부의 어떤 구조가 색깔을 가지고 있는지 생각해 본 적 있으신가요?",
    "summary": "엽록체 색소 탐구",
    "nodeType": "AI_ANSWER",
    "nodeTypeLabel": "AI 답변",
    "depth": 1,
    "createdAt": "2026-04-09T13:01:01"
  }
}
```

**nodeType 값:**

| 값 | 설명 | 직접 지정 가능 |
|---|---|---|
| `QUESTION` | 학생의 새 질문 | O |
| `REVISION` | 기존 이해 수정 | O |
| `AI_ANSWER` | AI 자동 생성 답변 | X |
| `INSIGHT` | AI 자동 생성 회고 | X |

> `REVISION` 사용 시 `parentNodeId`에 기존 AI_ANSWER 노드 ID를 지정하면 해당 노드의 자식으로 연결됩니다.

#### 마인드맵 트리 조회 예시

```http
GET /api/sessions/1/nodes/tree
```

```json
[
  {
    "id": 1,
    "content": "엽록체에서 실제로 빛을 흡수하는 분자는 무엇인가요?",
    "summary": "엽록체 빛 흡수 분자",
    "nodeType": "QUESTION",
    "nodeTypeLabel": "질문",
    "depth": 0,
    "children": [
      {
        "id": 2,
        "content": "좋은 질문입니다! ...",
        "summary": "엽록체 색소 탐구",
        "nodeType": "AI_ANSWER",
        "nodeTypeLabel": "AI 답변",
        "depth": 1,
        "children": []
      }
    ]
  }
]
```

#### 인사이트 생성 예시

```http
POST /api/sessions/1/nodes/insight
```

```json
{
  "id": 10,
  "content": "## 학습 회고 리포트\n\n**학습 출발점:** ...\n**사고 여정:** ...\n**이해 변화:** ...\n**핵심 연결고리:** ...\n**성장 포인트:** ...\n**다음 탐구 방향:** ...",
  "summary": "광합성 학습 회고",
  "nodeType": "INSIGHT",
  "nodeTypeLabel": "학습 회고",
  "createdAt": "2026-04-09T13:10:00"
}
```

---

## 프로젝트 구조

```
src/main/java/com/example/thinkmap/
├── ThinkmapApplication.java          # Spring Boot 진입점
├── client/
│   ├── GeminiClient.java             # Gemini API WebClient 호출 (429 재시도 포함)
│   └── dto/
│       ├── GeminiRequest.java        # Gemini API 요청 DTO
│       └── GeminiResponse.java       # Gemini API 응답 DTO
├── config/
│   ├── CorsConfig.java               # CORS 설정
│   ├── GlobalExceptionHandler.java   # 전역 예외 처리
│   ├── OpenApiConfig.java            # Swagger UI 브랜딩 설정
│   └── WebClientConfig.java          # WebClient 빈 설정 (타임아웃 120s)
├── controller/
│   ├── SessionController.java        # 세션 CRUD API
│   └── ThoughtController.java        # 노드·마인드맵·인사이트 API
├── domain/
│   ├── entity/
│   │   ├── LearningSession.java      # 학습 세션 엔티티 (@Index on updated_at)
│   │   ├── ThoughtNode.java          # 사고 노드 엔티티 (트리 구조)
│   │   └── NodeType.java             # QUESTION / AI_ANSWER / REVISION / INSIGHT
│   └── repository/
│       ├── LearningSessionRepository.java
│       └── ThoughtNodeRepository.java
├── dto/
│   ├── AddQuestionRequest.java       # 노드 추가 요청 (nodeType 유효성 검사 포함)
│   ├── AskResponse.java              # 질문+AI 답변 응답
│   ├── CreateSessionRequest.java     # 세션 생성 요청
│   ├── InsightResponse.java          # 인사이트 응답
│   ├── SessionResponse.java          # 세션 응답 (nodeCount/questionCount/hasInsight)
│   ├── ThoughtNodeResponse.java      # 노드 단건 응답
│   ├── ThoughtTreeNode.java          # 마인드맵 트리 노드 (재귀 children)
│   └── UpdateSessionTitleRequest.java
└── service/
    └── ThoughtService.java           # 핵심 비즈니스 로직
```

---

## AI 프롬프트 전략

### 소크라테스식 질문 유도 (QUESTION / REVISION)

Gemini에게 다음 원칙을 지시합니다:
- 직접 답변 금지 → 후속 탐구 질문으로 학생 스스로 생각하도록 유도
- `REVISION` 모드에서는 이해 변화를 인정하고 심화 탐구 방향 제시
- JSON 응답 강제: `{"summary": "...(80자 이내)", "answer": "..."}`
  - `summary`: 마인드맵 노드 라벨용 요약
  - `answer`: 소크라테스식 답변 본문

### 학습 회고 인사이트 (INSIGHT)

세션 전체 대화 맥락을 넘기고 6항목 리포트를 요청합니다:

1. 학습 출발점
2. 사고 여정
3. 이해 변화
4. 핵심 연결고리
5. 성장 포인트
6. 다음 탐구 방향

---

## 운영 환경 전환 (H2 → MySQL)

`application.yml`에서 주석 처리된 MySQL 설정을 활성화하세요:

```yaml
# 1. H2 datasource 설정 주석 처리
# 2. MySQL datasource 주석 해제
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: ${LOCAL_DB_URL}
  username: ${LOCAL_DB_USERNAME}
  password: ${LOCAL_DB_PASSWORD}

# 3. JPA ddl-auto를 update로 변경
jpa:
  hibernate:
    ddl-auto: update
```

MySQL 데이터베이스 생성:

```sql
CREATE DATABASE thinkmap_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## CORS 설정

`application.yml`에서 허용 오리진을 수정하세요:

```yaml
app:
  cors:
    origins: "http://localhost:5173,https://kit-vcoding.netlify.app,https://thinkmap-api.shop"
```

---

## 에러 응답 형식

모든 에러는 일관된 JSON 형식으로 반환됩니다:

```json
{
  "timestamp": "2026-04-09T13:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title: 제목은 필수입니다",
  "path": "/api/sessions"
}
```

| HTTP Status | 상황 |
|---|---|
| 400 | 입력값 유효성 오류, 잘못된 nodeType |
| 404 | 세션·노드를 찾을 수 없음 |
| 405 | 지원하지 않는 HTTP 메서드 |
| 500 | Gemini API 호출 실패 |

---

## 라이선스

본 프로젝트는 공모전 제출용으로 제작되었습니다.
