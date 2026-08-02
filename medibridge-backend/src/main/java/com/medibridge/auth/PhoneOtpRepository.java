package com.medibridge.auth;

import com.medibridge.auth.entity.PhoneOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Only ever the newest row for a number matters - requesting a code supersedes
 * whatever came before it. Ordered by id rather than created_at because
 * created_at is a TIMESTAMP with one-second resolution, and two codes issued in
 * the same second would have no defined order.
 */
public interface PhoneOtpRepository extends JpaRepository<PhoneOtp, Long> {

    Optional<PhoneOtp> findFirstByPhoneE164OrderByIdDesc(String phoneE164);

    /**
     * The same row, locked for the duration of the transaction.
     *
     * <p>Verification reads the attempt count, decides, then writes it back.
     * Two codes submitted at once would otherwise both read the same count and
     * both be allowed - which turns the attempt cap into a suggestion for
     * anyone willing to open two connections.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PhoneOtp o where o.phoneE164 = :phone order by o.id desc limit 1")
    Optional<PhoneOtp> lockLatestForPhone(@Param("phone") String phone);
}
