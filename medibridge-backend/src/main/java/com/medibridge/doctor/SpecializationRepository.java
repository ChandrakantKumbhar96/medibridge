package com.medibridge.doctor;

import com.medibridge.doctor.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {

    Optional<Specialization> findByNameIgnoreCase(String name);
}
