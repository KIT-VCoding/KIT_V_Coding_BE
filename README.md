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
| **사용자 격리** | 로그인한 사용자 본인의 세션만 조회·수정·삭제 가능 |

---

## 기술 스택

| 레이어 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.5 |
| Security | Spring Security 6, JWT, OAuth2 (Google·Kakao) |
| ORM | Spring Data JPA (Hibernate) |
| DB (개발) | H2 In-Memory |
| DB (운영) | MySQL 8 |
| AI | Google Gemini API (`gemini-2.5-flash`) |
| HTTP Client | Spring WebFlux WebClient (Reactor Netty) |
| API 문서 | SpringDoc OpenAPI 3 (Swagger UI) |
| 빌드 | Gradle 8 |

---

## 시작하기

### 사전 요구 사항

- JDK 17+
- Gemini API Key ([Google AI Studio](https://aistudio.google.com/app/apikey)에서 발급)
- Google OAuth2 Client ID/Secret (선택)
- Kakao OAuth2 Client ID/Secret (선택)

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하세요.

```properties
# Gemini API (필수)
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_BASE_URL=https://generativelanguage.googleapis.com
GEMINI_MODEL=gemini-2.5-flash

# JWT (필수 — 256비트 이상 랜덤 문자열 권장)
JWT_SECRET=thisIsAVeryLongSecretKeyForJwtTokenGenerationThatMustBeAtLeast256BitsLong!

# OAuth2 — Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# OAuth2 — Kakao
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

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

## 인증

모든 `/api/sessions/**` 엔드포인트는 **JWT 인증이 필수**입니다.  
요청 헤더에 토큰을 포함해야 합니다.

```http
Authorization: Bearer {accessToken}
```

### 인증 흐름

| 방식 | 엔드포인트 | 설명 |
|---|---|---|
| 자체 회원가입 | `POST /api/auth/signup` | 이메일·비밀번호 회원가입 |
| 자체 로그인 | `POST /api/auth/login` | 이메일·비밀번호 로그인 |
| Google OAuth2 | `GET /oauth2/authorization/google` | Google 로그인 리다이렉트 |
| Kakao OAuth2 | `GET /api/auth/kakao/url` → `GET /oauth2/authorization/kakao` | 카카오 로그인 URL 조회 |

OAuth2 로그인 성공 시 프론트 URL로 리다이렉트되며 `?token={jwt}` 쿼리 파라미터로 토큰이 전달됩니다.

---

## API 레퍼런스

전체 API는 실행 후 `http://localhost:8080/swagger-ui` 에서 인터랙티브하게 확인할 수 있습니다.

### 인증 (Auth)

| Method | Endpoint | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/api/auth/signup` | 불필요 | 자체 회원가입 |
| `POST` | `/api/auth/login` | 불필요 | 자체 로그인 |
| `GET` | `/api/auth/kakao/url` | 불필요 | 카카오 로그인 URL 조회 |
| `GET` | `/api/auth/me` | 필요 | 현재 로그인 사용자 정보 조회 |

#### 로그인 예시

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password1234"
}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### 학습 세션 (Sessions)

> 모든 엔드포인트에 `Authorization: Bearer {token}` 헤더 필요.  
> 본인이 소유한 세션만 조회·수정·삭제 가능합니다. 타인 세션 접근 시 **403** 반환.

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/sessions` | 새 학습 세션 생성 |
| `GET` | `/api/sessions` | 내 세션 목록 (최신 활동 순) |
| `GET` | `/api/sessions/{id}` | 세션 상세 조회 |
| `PATCH` | `/api/sessions/{id}` | 세션 제목 수정 |
| `DELETE` | `/api/sessions/{id}` | 세션 삭제 (하위 노드 포함 영구 삭제) |

#### 세션 생성 예시

```http
POST /api/sessions
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "광합성은 어떻게 빛을 에너지로 바꾸는가"
}
```

```json
{
  "id": 1,
  "userId": 42,
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

> 모든 엔드포인트에 `Authorization: Bearer {token}` 헤더 필요.

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/sessions/{sessionId}/nodes` | 질문·이해수정 노드 추가 + AI 답변 |
| `POST` | `/api/sessions/{sessionId}/nodes/insight` | 학습 회고 인사이트 생성 |
| `GET` | `/api/sessions/{sessionId}/nodes/tree` | 마인드맵 트리 조회 |

#### 질문 노드 추가 예시

```http
POST /api/sessions/1/nodes
Authorization: Bearer {token}
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
    "createdAt": "2026-04-09T13:01:00"
  },
  "answerNode": {
    "id": 2,
    "content": "좋은 질문입니다! 빛을 흡수하는 '주인공'을 찾고 계시군요. 그렇다면 엽록체 내부의 어떤 구조가 색깔을 가지고 있는지 생각해 본 적 있으신가요?",
    "summary": "엽록체 색소 탐구",
    "nodeType": "AI_ANSWER",
    "nodeTypeLabel": "AI 답변",
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

---

### AI 학습 포트폴리오 (Portfolio Report)

> `Authorization: Bearer {token}` 헤더 필요. 본인 세션에만 생성 가능.

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/sessions/{sessionId}/portfolio-report` | AI 포트폴리오 보고서 생성 |

세션에 저장된 학습 노드(질문·AI답변·이해수정·인사이트)를 바탕으로 10개 항목의 포트폴리오 보고서 JSON을 반환합니다.

```http
POST /api/sessions/1/portfolio-report
Authorization: Bearer {token}
```

```json
{
  "reportTitle": "광합성의 빛 에너지 변환 원리 탐구",
  "learningTopic": "엽록체의 구조와 광합성 메커니즘",
  "initialQuestion": "빛을 에너지로 바꾸는 분자에 대한 궁금증",
  "coreExplorationProcess": "...",
  "understandingTurningPoint": "...",
  "revisedUnderstanding": "...",
  "finalSummary": "...",
  "unresolvedConcepts": ["카로티노이드의 보조 역할"],
  "nextLearningQuestions": ["캘빈 회로는 어떻게 작동하는가?"],
  "oneLineReflection": "빛이 화학 에너지로 바뀌는 순간을 눈으로 본 것 같다"
}
```

---

## 프로젝트 구조

```
src/main/java/com/example/thinkmap/
├── ThinkmapApplication.java
├── client/
│   ├── GeminiClient.java                      # Gemini API WebClient 호출
│   └── dto/
│       ├── GeminiRequest.java
│       └── GeminiResponse.java
├── config/
│   ├── AppProperties.java                     # JWT·OAuth2·CORS 설정값 바인딩
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java            # 전역 예외 처리 (400·403·404·405·500)
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java                    # Spring Security + JWT 필터 설정
│   └── WebClientConfig.java
├── controller/
│   ├── AuthController.java                    # 회원가입·로그인·OAuth2 URL·내 정보
│   ├── PortfolioReportController.java         # AI 포트폴리오 보고서
│   ├── SessionController.java                 # 세션 CRUD
│   └── ThoughtController.java                 # 노드·마인드맵·인사이트
├── domain/
│   ├── entity/
│   │   ├── AuthProvider.java                  # LOCAL / GOOGLE / KAKAO
│   │   ├── LearningSession.java               # 학습 세션 (user FK 포함)
│   │   ├── NodeType.java                      # QUESTION / AI_ANSWER / REVISION / INSIGHT
│   │   ├── ThoughtNode.java                   # 사고 노드 (트리 구조)
│   │   └── User.java                          # 사용자 엔티티
│   └── repository/
│       ├── LearningSessionRepository.java
│       ├── ThoughtNodeRepository.java
│       └── UserRepository.java
├── dto/
│   ├── AddQuestionRequest.java
│   ├── AskResponse.java
│   ├── CreateSessionRequest.java
│   ├── InsightResponse.java
│   ├── PortfolioReportRequest.java
│   ├── PortfolioReportResponse.java
│   ├── SessionResponse.java                   # userId 필드 포함
│   ├── ThoughtNodeResponse.java
│   ├── ThoughtTreeNode.java
│   ├── UpdateSessionTitleRequest.java
│   └── auth/
│       ├── AuthResponse.java
│       ├── LoginRequest.java
│       ├── SignUpRequest.java
│       └── UserInfoResponse.java
├── security/
│   ├── UserPrincipal.java                     # Spring Security 인증 주체
│   ├── jwt/
│   │   ├── JwtAuthenticationFilter.java       # 요청마다 JWT 검증
│   │   └── JwtTokenProvider.java              # 토큰 생성·파싱·검증
│   └── oauth2/
│       ├── CustomOAuth2UserService.java
│       ├── HttpCookieOAuth2AuthorizationRequestRepository.java
│       ├── OAuth2AuthenticationFailureHandler.java
│       ├── OAuth2AuthenticationSuccessHandler.java # 성공 시 JWT 발급 후 FE 리다이렉트
│       └── userinfo/
│           ├── GoogleOAuth2UserInfo.java
│           ├── KakaoOAuth2UserInfo.java
│           ├── OAuth2UserInfo.java
│           └── OAuth2UserInfoFactory.java
└── service/
    ├── LocalAuthService.java                  # 자체 회원가입·로그인
    ├── PortfolioReportService.java
    └── ThoughtService.java                    # 세션·노드·인사이트 핵심 비즈니스 로직
```

---

## AI 프롬프트 전략

### 소크라테스식 질문 유도 (QUESTION / REVISION)

- 직접 답변 금지 → 후속 탐구 질문으로 학생이 스스로 생각하도록 유도
- `REVISION` 모드에서는 이해 변화를 인정하고 심화 탐구 방향 제시
- JSON 응답 강제: `{"summary": "...(30자 이내)", "answer": "..."}`
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

`application.yml`에서 H2 설정을 주석 처리하고 MySQL 설정을 활성화하세요.

```sql
CREATE DATABASE thinkmap_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 에러 응답 형식

모든 에러는 일관된 JSON 형식으로 반환됩니다:

```json
{
  "timestamp": "2026-04-09T13:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "해당 리소스에 접근 권한이 없습니다."
}
```

| HTTP Status | 상황 |
|---|---|
| 400 | 입력값 유효성 오류, 잘못된 nodeType |
| 401 | 인증 토큰 없음 또는 만료 |
| 403 | 타인 소유 세션 접근 시도 |
| 404 | 세션·노드를 찾을 수 없음 |
| 405 | 지원하지 않는 HTTP 메서드 |
| 500 | Gemini API 호출 실패 등 서버 내부 오류 |

---

## 라이선스

본 프로젝트는 공모전 제출용으로 제작되었습니다.
