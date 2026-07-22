package com.medibridge.prescription;

import com.medibridge.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    List<Prescription> findByPatientIdOrderByDateIssuedDesc(Integer patientId);

    Optional<Prescription> findByConsultationId(Integer consultationId);

    Optional<Prescription> findByIdAndPatientId(Integer id, Integer patientId);

    Optional<Prescription> findByIdAndDoctorId(Integer id, String doctorId);

    boolean existsByConsultationId(Integer consultationId);
}
