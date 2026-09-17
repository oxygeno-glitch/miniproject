# STUDIO G 배포 가이드

서버: `172.16.1.6` (도메인 없음, IP로 접속) / 인프라: Nginx

---

## 0. 서버에 설치되어 있어야 하는 것

- Java 21 (`java -version`으로 확인, 없으면 설치)
- PostgreSQL
- Nginx (이미 설치되어 있다고 하셨으니 생략)

빌드는 로컬 개발 PC에서 하고 결과물(jar, dist 폴더)만 서버로 옮기는 방식을 기준으로 적었습니다. 서버에 직접 Node.js/Gradle을 설치해서 서버에서 빌드해도 무방합니다.

---

## 1. PostgreSQL 준비 (서버에서)

```bash
sudo -u postgres psql
CREATE DATABASE project;
CREATE USER studiog WITH PASSWORD '원하는비번';
GRANT ALL PRIVILEGES ON DATABASE project TO studiog;
\q
```

기존 로컬 계정(`postgres` / `hw1234`)을 그대로 써도 동작은 하지만, 운영 DB는 전용 계정 + 강한 비밀번호를 쓰는 걸 권장합니다.

---

## 2. 백엔드 빌드 & 배포

**로컬(개발 PC)에서 빌드:**

```bash
cd project_backend
./gradlew bootJar
```

→ `build/libs/mini-0.0.1-SNAPSHOT.jar` 생성됨.

**서버로 전송 (PC에서 실행, scp 예시):**

```bash
ssh <서버계정>@172.16.1.6 "sudo mkdir -p /opt/project_backend && sudo chown <서버계정> /opt/project_backend"
scp build/libs/mini-0.0.1-SNAPSHOT.jar <서버계정>@172.16.1.6:/opt/project_backend/
```

**서버에서 환경변수 파일 생성:**

이 폴더의 `.env.example`을 서버의 `/opt/project_backend/.env`로 복사한 뒤 실제 DB 비밀번호로 수정:

```bash
sudo chmod 600 /opt/project_backend/.env
```

**systemd 서비스 등록:**

이 폴더의 `project-backend.service`를 서버의 `/etc/systemd/system/project-backend.service`로 복사:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now project-backend
sudo systemctl status project-backend
```

`Environment=SPRING_PROFILES_ACTIVE=prod` 덕분에 새로 추가한 `application-prod.yml` 설정(운영용 DB 접속 정보, `127.0.0.1:8080`만 리스닝)이 적용됩니다.

---

## 3. 프론트엔드 빌드 & 배포

**로컬에서 빌드:**

```bash
cd project_frontend
npm run build
```

→ `dist/` 폴더 생성됨. `axios`의 `baseURL`이 이미 상대경로 `/api`라서 프론트 코드는 수정할 필요가 없습니다 — Nginx가 같은 주소(`http://172.16.1.6`)에서 `/api`만 백엔드로 넘겨주기 때문입니다.

**서버에 폴더 준비 후 전송:**

```bash
ssh <서버계정>@172.16.1.6 "sudo mkdir -p /var/www/project_frontend/dist && sudo chown <서버계정> /var/www/project_frontend/dist"
scp -r dist/* <서버계정>@172.16.1.6:/var/www/project_frontend/dist/
```

---

## 4. Nginx 설정

이 폴더의 `nginx.conf`를 서버에 적용:

```bash
# Debian/Ubuntu 계열
sudo cp nginx.conf /etc/nginx/sites-available/project
sudo ln -s /etc/nginx/sites-available/project /etc/nginx/sites-enabled/

# 또는 conf.d 구조를 쓰는 배포판이면
sudo cp nginx.conf /etc/nginx/conf.d/project.conf

sudo nginx -t
sudo systemctl reload nginx
```

---

## 5. 방화벽

80번 포트만 외부에 열려있으면 됩니다. 8080(백엔드)은 `127.0.0.1`로만 열리도록 설정해서 외부에서 직접 접근할 수 없습니다.

```bash
sudo ufw allow 80/tcp   # ufw를 쓰는 경우
```

---

## 6. 접속 확인

브라우저에서 `http://172.16.1.6` 접속 → Vue 화면이 뜨고 로그인/데이터 조회가 정상 동작하면 성공입니다.

---

## 트러블슈팅

- **화면은 뜨는데 API 호출이 실패할 때**: `sudo systemctl status project-backend`, `sudo journalctl -u project-backend -f`로 백엔드 로그 확인.
- **새로고침(F5) 시 404**: `nginx.conf`의 `try_files $uri $uri/ /index.html;` 부분이 잘 적용됐는지 확인 (Vue Router 새로고침 대응용 설정입니다).
- **CORS 에러**: 지금 구조(Nginx가 프론트+API를 같은 주소로 묶음)에서는 발생하지 않습니다. 만약 프론트를 다른 포트로 따로 열어서 테스트한다면 그때만 발생할 수 있어요.
- **DB 연결 실패**: `.env`의 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`가 실제 PostgreSQL 계정과 일치하는지, PostgreSQL이 `localhost:5432`에서 뜨고 있는지 확인.

---

## 참고 (당장 필수는 아니지만 알아두면 좋은 것)

- 현재 `AuthController`가 비밀번호를 평문으로 저장/비교하고 있습니다. 학교 실습/데모용이면 문제 없지만, 나중에 진짜 외부에 공개할 계획이면 비밀번호 해시(BCrypt) 적용을 권장드려요.
- 도메인이 없어서 이번 배포는 HTTP(암호화 없음)로 진행합니다. 나중에 도메인을 얻으면 Let's Encrypt로 무료 HTTPS 인증서를 붙일 수 있어요.
