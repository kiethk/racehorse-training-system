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

    List<TrainingGroupMember> findByGroupIdAndLeftAtIsNullOrderByJoinedAtAsc(Long groupId);

    List<TrainingGroupMember> findByGroupIdInAndLeftAtIsNull(Collection<Long> groupIds);

    Optional<TrainingGroupMember> findByHorseIdAndLeftAtIsNull(Long horseId);

    List<TrainingGroupMember> findByHorseIdInAndLeftAtIsNull(Collection<Long> horseIds);

    long countByGroupIdAndLeftAtIsNull(Long groupId);

    /**
     * Danh sách hiển thị của nhiều nhóm: nhóm còn hoạt động lấy ngựa đang ở; nhóm Disbanded lấy những con rời đúng lúc giải tán.
     * (The roster shown for many groups: a live group shows its current horses; a Disbanded group the ones that left when it was disbanded.)
     */
    @Query("""
           SELECT m
           FROM TrainingGroupMember m
           JOIN TrainingGroup g ON m.groupId = g.id
           WHERE g.id IN :groupIds
             AND ((g.disbandedAt IS NULL AND m.leftAt IS NULL) OR m.leftAt = g.disbandedAt)
           ORDER BY m.joinedAt, m.id
           """)
    List<TrainingGroupMember> findRostersOfGroups(@Param("groupIds") Collection<Long> groupIds);

    void deleteByGroupId(Long groupId);

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
