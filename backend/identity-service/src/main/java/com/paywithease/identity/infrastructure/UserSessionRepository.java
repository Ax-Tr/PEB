package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.UserSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
  Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

  List<UserSession> findByUserIdAndStatus(String userId, UserSession.Status status);

  List<UserSession> findByFamilyId(String familyId);
}
