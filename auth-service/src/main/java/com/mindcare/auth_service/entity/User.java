package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "users", schema = "auth_schema")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String specialties;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "consultation_fee", precision = 12, scale = 2)
    private BigDecimal consultationFee;
}
