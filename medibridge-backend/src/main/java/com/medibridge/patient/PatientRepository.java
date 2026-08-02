package com.medibridge.patient;

import com.medibridge.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Integer> {

    Optional<Patient> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Patient> findByGoogleSub(String googleSub);

    /** Phone login's account lookup. Always pass a PhoneNumbers.toE164 value. */
    Optional<Patient> findByPhoneE164(String phoneE164);

    boolean existsByPhoneE164(String phoneE164);
}
