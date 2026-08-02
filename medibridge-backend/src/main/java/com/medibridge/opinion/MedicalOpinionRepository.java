package com.medibridge.opinion;

import com.medibridge.opinion.entity.MedicalOpinion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalOpinionRepository extends JpaRepository<MedicalOpinion, Integer> {

    boolean existsByAppointmentId(Integer appointmentId);

    /**
     * Ownership is expressed in the query, not checked after loading - the same
     * {@code (id, ownerId)} shape used everywhere else, so a miss is
     * indistinguishable from "does not exist" and returns 404 rather than 403.
     */
    @Query("""
           SELECT o FROM MedicalOpinion o
           WHERE o.id = :opinionId AND o.appointment.patient.id = :patientId
           """)
    Optional<MedicalOpinion> findByIdAndPatientId(@Param("opinionId") Integer opinionId,
                                                  @Param("patientId") Integer patientId);

    @Query("""
           SELECT o FROM MedicalOpinion o
           WHERE o.id = :opinionId AND o.appointment.doctor.id = :doctorId
           """)
    Optional<MedicalOpinion> findByIdAndDoctorId(@Param("opinionId") Integer opinionId,
                                                 @Param("doctorId") String doctorId);

    @Query("""
           SELECT o FROM MedicalOpinion o
           WHERE o.appointment.patient.id = :patientId
           ORDER BY o.issuedAt DESC
           """)
    List<MedicalOpinion> findForPatient(@Param("patientId") Integer patientId);
}
