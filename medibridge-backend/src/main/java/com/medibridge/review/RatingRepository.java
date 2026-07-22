package com.medibridge.review;

import com.medibridge.review.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {

    boolean existsByAppointmentId(Integer appointmentId);

    Optional<Rating> findByAppointmentId(Integer appointmentId);

    List<Rating> findByDoctorIdOrderByCreatedAtDesc(String doctorId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.doctor.id = :doctorId")
    Double averageStarsForDoctor(@Param("doctorId") String doctorId);

    long countByDoctorId(String doctorId);
}
