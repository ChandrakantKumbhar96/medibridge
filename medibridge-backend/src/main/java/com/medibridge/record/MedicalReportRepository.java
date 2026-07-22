package com.medibridge.record;

import com.medibridge.record.entity.MedicalReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalReportRepository extends JpaRepository<MedicalReport, Integer> {

    List<MedicalReport> findByPatientIdOrderByUploadDateDesc(Integer patientId);

    /** Ownership baked into the query - callers cannot fetch by id alone. */
    Optional<MedicalReport> findByIdAndPatientId(Integer id, Integer patientId);

    long countByPatientId(Integer patientId);
}
