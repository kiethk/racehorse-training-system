package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.PreventiveProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreventiveProtocolRepository extends JpaRepository<PreventiveProtocol, Long> {
    List<PreventiveProtocol> findByActiveTrueOrderByCategoryAscIdAsc();

    List<PreventiveProtocol> findByActiveTrueAndMandatoryTrue();

    Optional<PreventiveProtocol> findByCode(String code);
}
