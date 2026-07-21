CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    role_id INT REFERENCES roles (id),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo sẵn 3 Role gốc (Master Data)
INSERT INTO
    roles (name, description)
VALUES (
        'ROLE_USER',
        'Người dùng thông thường (Học sinh, sinh viên)'
    ),
    (
        'ROLE_EXPERT',
        'Chuyên gia tâm lý'
    ),
    (
        'ROLE_ADMIN',
        'Quản trị viên hệ thống'
    );
