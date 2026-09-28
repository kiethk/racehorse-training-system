package com.rtms.backend.repository;

import com.rtms.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.role.name = 'VETERINARIAN' ORDER BY u.id")
    List<User> findActiveVeterinarians();
}
