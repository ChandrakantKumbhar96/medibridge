package com.medibridge.review;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.review.dto.RatingRequest;
import com.medibridge.review.dto.RatingResponse;
import com.medibridge.review.entity.Rating;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RatingRepository ratingRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * A patient may review only their own, completed appointment, once.
     *
     * <p>Without the completed check, a patient could book, immediately review,
     * and cancel - which is how review systems get gamed.
     */
    @Transactional
    public RatingResponse submit(Integer patientId, RatingRequest request) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientId(request.appointmentId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "You can only review an appointment after it has been completed");
        }
        if (ratingRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("You have already reviewed this appointment");
        }

        Set<Rating.Highlight> highlights = new LinkedHashSet<>();
        if (request.whatStoodOut() != null) {
            for (String label : request.whatStoodOut()) {
                highlights.add(Rating.Highlight.fromLabel(label));
            }
        }

        Rating rating = Rating.builder()
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .stars(request.stars())
                .overallExperience(
                        Rating.OverallExperience.valueOf(request.overallExperience()))
                .highlights(highlights)
                .reviewText(request.reviewText())
                .build();

        rating = ratingRepository.save(rating);

        recalculateDoctorRating(appointment.getDoctor().getId());

        return toDto(rating);
    }

    /**
     * Denormalised average kept on the doctor row so listing pages need no
     * aggregate query per doctor. Recomputed from scratch rather than adjusted
     * incrementally, so it cannot drift.
     */
    private void recalculateDoctorRating(String doctorId) {
        Double average = ratingRepository.averageStarsForDoctor(doctorId);
        long count = ratingRepository.countByDoctorId(doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        doctor.setRatingAvg(average == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        doctor.setRatingCount((int) count);

        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> listForDoctor(String doctorId) {
        return ratingRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RatingResponse getForAppointment(Integer patientId, Integer appointmentId) {
        appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        return ratingRepository.findByAppointmentId(appointmentId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No review for this appointment"));
    }

    private RatingResponse toDto(Rating r) {
        return new RatingResponse(
                r.getId(),
                r.getAppointment().getId(),
                r.getDoctor().getId(),
                r.getDoctor().getFullName(),
                r.getPatient().getFullName(),
                r.getStars(),
                r.getOverallExperience().name(),
                r.getHighlights().stream().map(Rating.Highlight::getLabel).toList(),
                r.getReviewText(),
                r.getCreatedAt() == null ? null : r.getCreatedAt().format(DATE));
    }
}
