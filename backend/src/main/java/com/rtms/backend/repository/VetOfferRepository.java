package com.rtms.backend.repository;

import com.rtms.backend.entity.VetOffer;
import com.rtms.backend.enums.VetOfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VetOfferRepository extends JpaRepository<VetOffer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vo FROM VetOffer vo WHERE vo.id = :id")
    Optional<VetOffer> findByIdForUpdate(@Param("id") Long id);

    List<VetOffer> findByCareScheduleId(Long careScheduleId);

    Optional<VetOffer> findFirstByCareScheduleIdAndStatus(Long careScheduleId, VetOfferStatus status);

    Optional<VetOffer> findFirstByCareScheduleIdOrderByRoundDesc(Long careScheduleId);

    List<VetOffer> findByVeterinarianIdAndStatus(Long veterinarianId, VetOfferStatus status);

    Page<VetOffer> findByVeterinarianId(Long veterinarianId, Pageable pageable);

    Page<VetOffer> findByVeterinarianIdAndStatus(Long veterinarianId, VetOfferStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<VetOffer> findByStatusAndExpiresAtBefore(VetOfferStatus status, LocalDateTime dateTime);

    boolean existsByCareScheduleIdAndVeterinarianIdAndRound(Long careScheduleId, Long veterinarianId, int round);
}