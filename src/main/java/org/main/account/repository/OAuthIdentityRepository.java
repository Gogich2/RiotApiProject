package org.main.account.repository;

import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.OAuthIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubjectId(
            String provider,
            String providerSubjectId
    );
}
