package com.medibridge.doctor;

import com.medibridge.common.enums.AccountStatus;
import com.medibridge.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, String> {

    Optional<Doctor> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    List<Doctor> findAllByStatus(AccountStatus status);

    @Query("""
           SELECT d FROM Doctor d
           WHERE d.status = :status
             AND (:specializationId IS NULL OR d.specialization.id = :specializationId)
             AND (:search IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    List<Doctor> search(@Param("status") AccountStatus status,
                        @Param("specializationId") Integer specializationId,
                        @Param("search") String search);

    long countByStatus(AccountStatus status);
}
