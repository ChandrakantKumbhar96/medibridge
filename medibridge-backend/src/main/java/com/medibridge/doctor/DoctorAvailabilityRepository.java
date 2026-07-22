package com.medibridge.doctor;

import com.medibridge.doctor.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Integer> {

    List<DoctorAvailability> findByDoctorIdOrderByDayOfWeek(String doctorId);

    Optional<DoctorAvailability> findByDoctorIdAndDayOfWeek(String doctorId, DayOfWeek dayOfWeek);
}
