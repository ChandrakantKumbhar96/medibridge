package com.medibridge.prescription;

import com.medibridge.prescription.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<ConsultationRecord, Integer> {

    Optional<ConsultationRecord> findByAppointmentId(Integer appointmentId);

    boolean existsByAppointmentId(Integer appointmentId);
}
