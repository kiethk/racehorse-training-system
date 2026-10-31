package com.rtms.backend.training.repository;

import com.rtms.backend.training.dto.HorseStallInfo;
import com.rtms.backend.training.entity.TrainingGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingGroupMemberRepository extends JpaRepository<TrainingGroupMember, Long> {

    List<TrainingGroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    List<TrainingGroupMember> findByGroupIdIn(Collection<Long> groupIds);

    Optional<TrainingGroupMember> findByHorseId(Long horseId);

    List<TrainingGroupMember> findByHorseIdIn(Collection<Long> horseIds);

    long countByGroupId(Long groupId);

    /**
     * Chuồng và khu của nhiều chiến mã bằng MỘT câu JOIN. Ngựa chưa có chuồng sẽ không xuất hiện.
     * (Stall and area of many horses with ONE JOIN. A horse without a stall does not appear.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.HorseStallInfo(h.id, ss.stallCode, ss.groomId, a.code, a.trainerId)
           FROM Horse h
           JOIN StableStall ss ON h.currentStallId = ss.id
           JOIN Area a ON ss.areaId = a.id
           WHERE h.id IN :horseIds
           """)
    List<HorseStallInfo> findStallInfoByHorseIds(@Param("horseIds") Collection<Long> horseIds);

    /** Mọi chiến mã đang ở trong các khu do Trainer phụ trách. (Every horse housed in the areas a Trainer manages.) */
    @Query("""
           SELECT h.id
           FROM Horse h
           JOIN StableStall ss ON h.currentStallId = ss.id
           JOIN Area a ON ss.areaId = a.id
           WHERE a.trainerId = :trainerId
           ORDER BY h.name
           """)
    List<Long> findHorseIdsInAreasOf(@Param("trainerId") Long trainerId);
}
