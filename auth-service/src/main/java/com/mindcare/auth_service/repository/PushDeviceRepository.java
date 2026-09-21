package com.mindcare.auth_service.repository;
import com.mindcare.auth_service.entity.PushDevice; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface PushDeviceRepository extends JpaRepository<PushDevice,UUID>{Optional<PushDevice> findByUserIdAndInstallationId(UUID userId,String installationId);Optional<PushDevice> findByPushToken(String pushToken);List<PushDevice> findByUserIdAndEnabledTrue(UUID userId);}
