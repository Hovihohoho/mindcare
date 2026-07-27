# Cấu trúc dự án Booking Service

Repository dùng package theo feature:

```text
src/main/java/com/mindcare/bookingservice/
├── BookingServiceApplication.java
├── schedule/
├── booking/
├── payment/
├── chat/
├── consultation/
├── review/
├── integration/
│   └── auth/
└── shared/
    ├── config/
    ├── dto/
    ├── exception/
    ├── security/
    └── web/
```

Mỗi feature chỉ tạo package con khi thực sự cần.

## Ranh giới package

- Component nghiệp vụ nằm trong feature sở hữu.
- `shared` chỉ dành cho thành phần thật sự dùng bởi ít nhất hai feature và không chứa rule riêng.
- Feature không truy cập repository/entity nội bộ feature khác để ghép nghiệp vụ.
- Tích hợp chéo feature dùng service contract/application facade rõ ràng.
- Payment provider adapter đặt dưới `payment/integration` hoặc `integration/payment`, không để SDK/provider leak vào domain.

## Luồng dependency

```text
controller / websocket adapter
          |
      service interface
          |
 service implementation / domain
          |
 repository + external ports
          |
 JPA / provider adapters
```

Controller mỏng; transaction ở service. Repository chỉ persistence. Mapper chỉ mapping cơ học.

## Test

```text
src/test/java/com/mindcare/bookingservice/
├── migration/
├── schedule/
├── booking/
├── payment/
├── chat/
├── consultation/
├── review/
├── shared/
└── support/
```

Testcontainers configuration nằm trong `support`, không đưa vào main source.
