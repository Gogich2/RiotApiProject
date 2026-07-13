package org.main.account.repository;

import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByEmailNormalized(String emailNormalized);
}
