# git push
```
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/SEOMOONJEONG/korit_07_project_nuzip.git
git push -u origin main
```


# Nuzip
## Domain(핵심 데이터 모델)
- AuthProvider: 로그인 제공자 구분(enum) — LOCAL, GOOGLE 등.
- NewsCategory: 뉴스 카테고리(enum) — POLITICS, IT_SCIENCE 등.
- User: 사용자 엔티티(JPA). DB 테이블과 1:1로 매핑.
- UserRepository: User 엔티티용 JPA 리포지토리(조회/저장/중복검사 등).

## dto (요청/응답 전용 데이터 그릇)
- AccountCredentialsDto: 아이디/비밀번호 같은 자격 증명 전달용.
- EditFormMetaRequestDto / EditFormMetaResponseDto: 프로필 수정 화면에 필요한 메타데이터 요청/응답.
- GoogleTokenRequest: 구글에서 받은 토큰을 서버로 보낼 때의 요청 형태.
- LoginResponseDto: 로그인 성공 시 내려주는 결과(예: JWT, 사용자 요약정보).
- RegisterRequestDto / RegisterResponseDto: 회원가입 요청/응답 데이터.
- UpdateMyInfoRequestDto: 내 정보(닉네임, 전화 등) 수정 요청.
- VerifyPasswordRequestDto / VerifyPasswordResponseDto: 비밀번호 재확인 요청/응답.

## security (인증/인가 전담)
- AuthEntryPoint: 인증 실패·권한 없음일 때 표준 에러 응답을 만드는 엔트리포인트.
- CustomOAuth2SuccessHandler: 구글 OAuth2 로그인 성공 후 후처리(유저 생성/토큰 발급/리다이렉트 등).
- GoogleTokenVerifier: 구글 ID 토큰의 서명·만료 등 검증 로직.
- JwtAuthenticationFilter: 매 요청마다 Authorization 헤더의 JWT를 파싱해 인증 컨텍스트에 주입.
- JwtService: 액세스/리프레시 토큰 생성·검증·클레임 처리.
- OAuthJoinService: OAuth2 최초 로그인 시 계정 프로비저닝(신규 유저 생성/기본값 설정).
- SecurityConfig: Spring Security 전체 설정(CORS, 세션정책, 필터 체인, 허용 경로 등).
- UserDetailsServiceImpl: Spring Security가 요구하는 사용자 조회 구현(LOCAL 로그인용).

## service (도메인 비즈니스 로직)
- GoogleAuthService: 구글 인증 관련 도메인 로직(토큰 검증 → 유저 매핑 등).
- UserService: 회원가입, 로그인, 프로필 수정, 카테고리 관리, 비번 변경 같은 사용자 비즈니스 로직.

## web (HTTP 엔드포인트 & 예외 처리)
- AuthController: 로컬 로그인/회원가입/토큰 재발급 등 인증 관련 REST API.
- GoogleAuthController: 프론트가 구글 토큰을 전달하거나 OAuth2 콜백을 받을 때 쓰는 API.
- LoginController: (프로젝트 이력에 따라) 전통적인 로그인 엔드포인트 분리 운용 시 사용.
- UserController: 내 정보 조회/수정, 카테고리 저장·조회 등 사용자 기능 API.
- ApiExceptionHandler / GlobalExceptionHandler: 컨트롤러 전역 예외 처리(@RestControllerAdvice).


# 기능 별 흐름
## 인증 플로우 개요
### 공통 준비
사용자는 LOCAL 자격 증명(이메일·비밀번호) 또는 OAUTH 제공자(예: Google, Kakao 등)를 선택합니다.
서버는 두 흐름을 분기 처리하며, 성공 시 동일한 세션/토큰 관리 정책을 적용합니다.

## 회원가입(Sign-up)
### LOCAL
회원가입 요청 → 필수 정보 검사(이메일 중복, 비밀번호 규칙 등) → 비밀번호 해시 → 사용자 레코드 생성 → 필요 시 이메일 인증 절차 → 가입 완료 응답.
### OAUTH
외부 로그인 시도 → OAuth 제공자 인증 화면 → 인증 성공 시 제공자 토큰 수신 → 제공자 사용자 정보 확인 → 우리 서비스 DB에 해당 제공자 ID 등록 여부 확인 → (없으면) 내부 사용자 레코드 생성(별도 비밀번호 없이 OAuth 메타데이터 저장) → 가입 완료/로그인 상태로 리다이렉트.

## 로그인(Login)
### LOCAL
로그인 폼 제출 → 이메일·비밀번호 검증 → 비밀번호 해시 비교 → 성공 시 세션/토큰 발급 → 로그인 응답(대시보드 등으로 이동).
### OAUTH
OAuth 로그인 버튼 → 제공자 인증 화면 → 성공 시 callback 처리 → 제공자 토큰 검증 → 등록된 사용자 매핑 → 세션/토큰 발급 → 로그인 완료 리다이렉트.

## 회원정보 수정(Profile Update)
### LOCAL
정보 수정 요청(프로필/비밀번호 등) → 현재 세션/토큰으로 본인 확인 → 변경 필드 유효성 검사 → 필요 시 비밀번호 재확인 → DB 업데이트 → 성공 응답.
### OAUTH
정보 수정 요청 → 세션/토큰으로 본인 확인 → 수정 가능한 항목만 허용(대개 별칭, 연락처 등) → OAuth 제공자가 관리하는 항목(이메일 등)은 직접 수정 불가, 재인증이나 제공자 대시보드 안내 → DB 업데이트 → 성공 응답.

## 흐름 요약 도식 (텍스트 기반)
```
[사용자]
├─ 회원가입 ──▶ (LOCAL) 입력 검증 → 비밀번호 해시 → DB 저장 → 완료
│              (OAUTH) 제공자 인증 → 토큰 검증 → DB 연동 → 완료
│  
├─ 로그인 ─────▶ (LOCAL) 자격 검증 → 세션/토큰 발급 → 완료
│               (OAUTH) 제공자 인증 → 토큰 검증 → 세션/토큰 발급 → 완료
│  
└─ 정보 수정 ──▶ (LOCAL) 본인 인증 → 변경 유효성 검사 → DB 업데이트
                (OAUTH) 본인 인증 → 허용 항목 수정 → DB 업데이트
```
