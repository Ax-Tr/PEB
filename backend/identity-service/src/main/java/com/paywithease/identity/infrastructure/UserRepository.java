package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByMobileHash(String mobileHash);

  boolean existsByMobileHash(String mobileHash);
}
