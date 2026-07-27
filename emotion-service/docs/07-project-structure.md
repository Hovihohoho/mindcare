# Cấu trúc dự án

## Hiện trạng

```text
emotion-service/
|-- pom.xml
|-- mvnw, mvnw.cmd
|-- AGENTS.md
|-- 00-system-context.md
|-- docs/
|-- src/
|   |-- main/
|   |   |-- java/com/mindcare/emotionservice/
|   |   |   |-- EmotionServiceApplication.java
|   |   |   |-- journal/
|   |   |   |-- healthmetric/
|   |   |   |-- assessment/
|   |   |   |-- risk/
|   |   |   `-- shared/
|   |   `-- resources/
|   |       |-- application.yml
|   |       `-- db/migration/V1__... đến V3__...
|   `-- test/
|       |-- java/com/mindcare/emotionservice/   # context + unit test theo feature
|       `-- resources/application-test.yml      # PostgreSQL metadata-offline
`-- target/                         # build output, không commit
```

Repository tổ chức theo feature; implementation mới phải được bổ sung vào feature sở hữu nghiệp vụ.

## Cấu trúc package của dự án

```text
com.mindcare.emotionservice
|-- EmotionServiceApplication
|-- journal
|   |-- controller
|   |-- dto
|   |-- entity
|   |-- mapper
|   |-- repository
|   `-- service
|-- healthmetric
|   |-- controller
|   |-- dto
|   |-- entity
|   |-- mapper
|   |-- repository
|   `-- service
|-- assessment
|   |-- controller
|   |-- dto
|   |-- entity
|   |-- mapper
|   |-- repository
|   `-- service
|-- risk
|   |-- controller
|   |-- dto
|   |-- entity
|   |-- mapper
|   |-- repository
|   `-- service
`-- shared
    |-- dto
    |-- exception
    |-- security
    |-- util
    |-- validation
    `-- web
```

Chỉ tạo package con khi có file thực tế. `shared` không phải nơi chứa tiện ích tùy ý; thành phần ở đây phải độc lập với nghiệp vụ của từng feature và được nhiều feature sử dụng.

## Trách nhiệm layer

### Trong từng feature

- `controller`: parse/validate transport input, lấy principal, gọi service và map HTTP status.
- `dto`: request/response của riêng feature, không dùng làm JPA entity.
- `entity`: persistence model và invariant cục bộ của feature.
- `mapper`: mapping cơ học DTO/entity; không chứa luật nghiệp vụ.
- `repository`: persistence của feature; không bị controller gọi trực tiếp.
- `service`: use case, transaction và orchestration của feature.

### Shared

- Chứa error envelope, pagination, config, event envelope và cross-cutting concerns thật sự dùng chung.
- `shared/security` chứa pre-auth principal/filter/SecurityFilterChain; `shared/validation` chứa constraint dùng chung không mang nghiệp vụ riêng của feature; `shared/web` chứa correlation filter và global exception mapping.
- Không chứa Entity/Repository nghiệp vụ hoặc DTO chỉ phục vụ một feature.
- Không để `shared` phụ thuộc ngược vào `journal`, `healthmetric`, `assessment` hoặc `risk`.

## Resources và test

```text
src/main/resources/
|-- application.yml
|-- application-local.yml          # chỉ giá trị không bí mật
`-- db/migration/
    |-- V1__Init_Emotion_Schema.sql
    |-- V2__Use_Application_UUID_And_Timestamptz.sql
    `-- V3__Add_Service_Implementation_Prerequisites.sql

src/test/java/com/mindcare/emotionservice/
|-- journal/                       # mirror feature production package
|-- healthmetric/
|-- assessment/
|-- risk/
|-- shared/
|-- migration/                     # Flyway clean/upgrade integration test
|-- support/                       # shared Testcontainers configuration
`-- architecture/                  # optional ArchUnit rules

src/test/resources/
|-- application-test.yml           # metadata-offline context test
`-- application-integration.yml    # PostgreSQL Testcontainers profile
```

- Test package mirror production package.
- Fixtures dùng builder/mother với dữ liệu giả không nhận diện cá nhân.
- Integration test DB dùng PostgreSQL Testcontainers (`postgres:16.8-alpine`); H2 không được dùng để kiểm tra JSONB/schema/SQL PostgreSQL.

## Dependency direction

```text
feature/controller -> feature/service -> feature/repository -> feature/entity
feature/mapper     -> feature/dto + feature/entity
feature/*          -> shared/*
```

Controller không gọi repository trực tiếp. Entity không phụ thuộc controller, DTO hoặc service. Một feature không import repository/entity của feature khác; khi cần phối hợp, dùng service contract hoặc event đã được thiết kế rõ.

## Khi thêm capability mới

1. Xác nhận ownership trong system context.
2. Viết/điều chỉnh contract và business rule.
3. Tạo entity và service/use case nhỏ nhất trong package tương ứng.
4. Thêm migration nếu cần, sau đó repository.
5. Thêm controller/event integration khi thuộc phạm vi.
6. Unit, integration, security/contract test.
7. Cập nhật OpenAPI và docs/decision.
