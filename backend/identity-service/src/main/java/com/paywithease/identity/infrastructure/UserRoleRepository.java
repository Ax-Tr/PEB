package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.PK> {

  /** Role names granted to a user (used as {@code ROLE_*} authorities). */
  @Query(
      value =
          "SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = :userId",
      nativeQuery = true)
  List<String> findRoleNames(@Param("userId") String userId);

  /** Fine-grained permission codes for a user (via role_permissions). */
  @Query(
      value =
          "SELECT DISTINCT p.code FROM permissions p "
              + "JOIN role_permissions rp ON rp.permission_id = p.id "
              + "JOIN user_roles ur ON ur.role_id = rp.role_id "
              + "WHERE ur.user_id = :userId",
      nativeQuery = true)
  List<String> findPermissionCodes(@Param("userId") String userId);
}
