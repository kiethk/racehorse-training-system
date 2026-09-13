# SETUP_NOTES.md — Ghi chú setup dự án

> Tổng hợp các lỗi đã gặp trong quá trình setup skeleton ban đầu và cách fix.
> Đọc file này trước khi bắt đầu để tránh mất thời gian debug lại những vấn đề đã biết.

---

## Checklist setup ban đầu

Trước khi chạy dự án lần đầu, cần:

- [ ] Cài **Docker Desktop** (để chạy PostgreSQL qua `docker-compose.yml`)
- [ ] Cài **JDK 21**
- [ ] Cài **Node.js 18+**
- [ ] Copy `frontend/.env.local.example` → `frontend/.env.local`

```bash
# Khởi động PostgreSQL
docker-compose up -d

# Chạy backend (port 8080)
cd backend && ./mvnw spring-boot:run

# Chạy frontend (port 3000)
cd frontend && npm install && npm run dev
```

---

## Lỗi 1: Timezone "Asia/Saigon" khi kết nối PostgreSQL từ Spring Boot

**Triệu chứng:** Spring Boot khởi động lỗi hoặc JPA/Hibernate báo warning liên quan timezone, đặc biệt trên Windows/WSL.

**Nguyên nhân:** JVM mặc định trên Windows có thể không nhận timezone ID `Asia/Saigon` (tên cũ). PostgreSQL dùng `Asia/Ho_Chi_Minh`.

**Cách fix:** Truyền timezone vào JVM arguments trong `pom.xml`:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-Duser.timezone=Asia/Ho_Chi_Minh</jvmArguments>
    </configuration>
</plugin>
```

Và trong `application.properties`:
```properties
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh
```

---

## Lỗi 2: Flyway không tự chạy trên Spring Boot 4.x

**Triệu chứng:** Thêm dependency `flyway-core` vào `pom.xml` nhưng Flyway không chạy, không tạo bảng, không log gì về migration.

**Nguyên nhân:** Spring Boot 4.x tách Flyway auto-configuration ra module riêng. Nếu chỉ thêm `flyway-core` thì thiếu auto-config bean.

**Cách fix:** Dùng `spring-boot-starter-flyway` thay vì `flyway-core`:

```xml
<!-- ĐÚNG -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>

<!-- Nếu dùng PostgreSQL, thêm thêm: -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Lưu ý Flyway migration:**
- File đặt tại `src/main/resources/db/migration/`
- Tên file: `V{số}__{mô_tả}.sql` (hai dấu gạch dưới)
- **KHÔNG chỉnh sửa file migration đã commit/chạy** — Flyway lưu checksum, nếu sửa sẽ báo lỗi khi restart backend. Muốn thay đổi schema → tạo file `V2__...` mới.

---

## Lỗi 3: Swagger UI redirect về /login

**Triệu chứng:** Truy cập `http://localhost:8080/swagger-ui/index.html` bị redirect về `/login`.

**Nguyên nhân:** Spring Security mặc định yêu cầu xác thực cho tất cả endpoint, kể cả Swagger.

**Cách fix:** Permit Swagger endpoints trong `SecurityConfig.java`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/api/**")
    .permitAll()
    .anyRequest().authenticated())
```

Xem chi tiết tại: `backend/src/main/java/com/rtms/backend/config/SecurityConfig.java`

---

## Lỗi 4: CORS khi frontend (localhost:3000) gọi backend (localhost:8080)

**Triệu chứng:** Browser báo lỗi CORS policy, request bị block, mặc dù đã thêm `@CrossOrigin` trên controller.

**Nguyên nhân:** Khi có Spring Security, `@CrossOrigin` trên Controller không hoạt động đúng vì Security filter chặn trước khi request tới Controller.

**Cách fix:** Cấu hình CORS qua `CorsConfigurationSource` bean trong `SecurityConfig`, KHÔNG dùng `@CrossOrigin`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

// Và trong securityFilterChain:
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

Xem chi tiết tại: `backend/src/main/java/com/rtms/backend/config/SecurityConfig.java`

---

## Lỗi 5: IDE báo sai package name (lỗi "main.java.com.rtms...")

**Triệu chứng:** VS Code (Java extension) báo lỗi package phải là `main.java.com.rtms.backend.config` thay vì `com.rtms.backend.config`.

**Nguyên nhân:** IDE chưa nhận `src/main/java` là Source Root. Package Java đúng là `com.rtms.backend...` — không có "main.java" trong package name.

**Cách fix (VS Code):**
1. `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace` → **Restart and Delete**
2. Hoặc chạy: `./mvnw eclipse:eclipse` trong thư mục `backend/` để tạo `.classpath` file
3. Đảm bảo mở đúng workspace là thư mục `backend/` (chứa `pom.xml`), không phải thư mục cha

**Cách fix (IntelliJ):**
- Chuột phải vào `src/main/java` → **Mark Directory As** → **Sources Root**

**Lưu ý:** KHÔNG đổi package name theo gợi ý của IDE — code đang đúng, IDE mới bị nhầm.

---

## Cấu trúc dự án

```
racehorse-training-system/
├── docker-compose.yml          # PostgreSQL container
├── backend/                    # Spring Boot (port 8080)
│   ├── pom.xml
│   └── src/main/java/com/rtms/backend/
│       ├── config/             # SecurityConfig (CORS, Security rules)
│       ├── controller/         # REST Controllers → /api/...
│       ├── dto/                # Response wrappers (ApiResponse<T>)
│       ├── entity/             # JPA Entities
│       └── repository/         # Spring Data JPA Repositories
└── frontend/                   # Next.js (port 3000)
    ├── .env.local.example      # Copy → .env.local trước khi chạy
    └── src/
        ├── services/api.ts     # HTTP client chung (apiGet, apiPost)
        └── types/              # TypeScript interfaces cho từng entity
```
