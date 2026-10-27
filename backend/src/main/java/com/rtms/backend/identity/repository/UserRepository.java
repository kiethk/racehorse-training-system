package com.rtms.backend.identity.repository;
import com.rtms.backend.identity.entity.TrainerProfile;
import com.rtms.backend.identity.entity.VeterinarianProfile;

import com.rtms.backend.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.id")
    List<User> findByIdsForUpdate(@Param("ids") Collection<Long> ids);

    @Query("SELECT u FROM User u JOIN VeterinarianProfile vp ON vp.userId = u.id "
            + "WHERE u.isActive = true AND u.role.name = 'VETERINARIAN' "
            + "AND vp.licenseNumber IS NOT NULL AND TRIM(vp.licenseNumber) <> '' ORDER BY u.id")
    List<User> findActiveVeterinarians();

    @Query("SELECT u FROM User u JOIN TrainerProfile tp ON tp.userId = u.id "
            + "WHERE u.isActive = true AND u.role.name = 'HEAD_TRAINER' "
            + "AND tp.certificationNumber IS NOT NULL AND TRIM(tp.certificationNumber) <> '' ORDER BY u.id")
    List<User> findActiveHeadTrainers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND u.role.name = 'CLUB_MANAGER'")
    long countActiveClubManagers();

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.role.name = 'CLUB_MANAGER'")
    List<User> findActiveClubManagers();
}
