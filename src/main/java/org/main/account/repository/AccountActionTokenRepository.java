package org.main.account.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.AccountActionTokenEntity;
import org.main.account.entity.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionTokenEntity, UUID> {

    @Query("""
            select token from AccountActionTokenEntity token
            where token.tokenHash = :tokenHash
              and token.tokenType = :tokenType
              and token.consumedAt is null
              and token.expiresAt > :now
            """)
    Optional<AccountActionTokenEntity> findUsableByTokenHashAndTokenType(
            @Param("tokenHash") String tokenHash,
            @Param("tokenType") AccountTokenType tokenType,
            @Param("now") OffsetDateTime now
    );
}
