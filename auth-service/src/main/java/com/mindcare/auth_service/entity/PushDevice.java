package com.mindcare.auth_service.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime; import java.util.UUID; import lombok.Getter; import lombok.NoArgsConstructor;
@Getter @Entity @Table(name="push_devices", schema="auth_schema") @NoArgsConstructor
public class PushDevice {
 @Id private UUID id; @Column(name="user_id",nullable=false) private UUID userId; @Column(name="installation_id",nullable=false,length=120) private String installationId; @Column(name="push_token",nullable=false,length=255,unique=true) private String pushToken; @Column(nullable=false,length=20) private String platform; @Column(nullable=false) private boolean enabled; @Column(name="last_seen_at",nullable=false) private OffsetDateTime lastSeenAt; @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 public static PushDevice create(UUID userId,String installationId,String token,String platform){var v=new PushDevice();v.id=UUID.randomUUID();v.userId=userId;v.installationId=installationId;v.createdAt=OffsetDateTime.now();v.refresh(token,platform);return v;}
 public void refresh(String token,String platform){this.pushToken=token;this.platform=platform;this.enabled=true;this.lastSeenAt=OffsetDateTime.now();}
 public void claim(UUID userId,String installationId,String platform){this.userId=userId;this.installationId=installationId;refresh(this.pushToken,platform);}
 public void disable(){this.enabled=false;this.lastSeenAt=OffsetDateTime.now();}
}
