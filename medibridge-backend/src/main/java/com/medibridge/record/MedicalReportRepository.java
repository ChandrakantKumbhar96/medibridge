package com.medibridge.record;

import com.medibridge.record.entity.MedicalReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalReportRepository extends JpaRepository<MedicalReport, Integer> {

    /** Everything on the account: the holder's own documents and every dependent's. */
    List<MedicalReport> findByPatientIdOrderByUploadDateDesc(Integer patientId);

    /** Just one dependent's documents. */
    List<MedicalReport> findByPatientIdAndFamilyMemberIdOrderByUploadDateDesc(
            Integer patientId, Integer familyMemberId);

    /**
     * The account holder's own documents. Spring Data cannot express "IS NULL" in
     * a derived name without this suffix, and reusing the plain patient query
     * would silently fold every dependent's file into the holder's list.
     */
    List<MedicalReport> findByPatientIdAndFamilyMemberIsNullOrderByUploadDateDesc(Integer patientId);

    /** Ownership baked into the query - callers cannot fetch by id alone. */
    Optional<MedicalReport> findByIdAndPatientId(Integer id, Integer patientId);

    /** Everything on the account, dependents included - the dashboard's tile. */
    long countByPatientId(Integer patientId);

    long countByPatientIdAndFamilyMemberId(Integer patientId, Integer familyMemberId);

    long countByPatientIdAndFamilyMemberIsNull(Integer patientId);
}
