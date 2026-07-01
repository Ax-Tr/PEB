package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.Device;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
  Optional<Device> findByUserIdAndDeviceHash(String userId, String deviceHash);
}
