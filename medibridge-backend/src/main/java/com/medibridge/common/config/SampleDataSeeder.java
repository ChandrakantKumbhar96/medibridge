package com.medibridge.common.config;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.PaymentStatus;
import com.medibridge.doctor.DoctorAvailabilityRepository;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.DoctorScheduleRepository;
import com.medibridge.doctor.SpecializationRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorAvailability;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.doctor.entity.Specialization;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import com.medibridge.payment.PaymentRepository;
import com.medibridge.payment.entity.PaymentTransaction;
import com.medibridge.prescription.ConsultationRepository;
import com.medibridge.prescription.PrescriptionRepository;
import com.medibridge.prescription.entity.ConsultationRecord;
import com.medibridge.prescription.entity.Prescription;
import com.medibridge.prescription.entity.PrescriptionItem;
import com.medibridge.review.RatingRepository;
import com.medibridge.review.entity.Rating;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Realistic demo data.
 *
 * <p>Written in Java rather than a SQL migration for two reasons: passwords are
 * hashed by the same {@link PasswordEncoder} the login path verifies against
 * (a hand-written BCrypt string in SQL is unverifiable), and appointment dates
 * are relative to today, so the demo still looks current months from now.
 *
 * <p>Idempotent - it does nothing if doctors already exist. Disable entirely
 * with {@code medibridge.seed-sample-data=false}.
 */
@Configuration
@RequiredArgsConstructor
public class SampleDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private static final String DEMO_PASSWORD = "Test@1234";

    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final RatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${medibridge.seed-sample-data:true}")
    private boolean enabled;

    /** Order 2 so the admin from DataSeeder is already in place. */
    @Bean
    @Order(2)
    public ApplicationRunner seedSampleData() {
        return args -> {
            if (!enabled) {
                return;
            }
            seed();
        };
    }

    @Transactional
    public void seed() {
        Map<String, Specialization> specs = new HashMap<>();
        specializationRepository.findAll().forEach(s -> specs.put(s.getName(), s));

        // Idempotent per row: new doctors/patients are added on restart, and
        // existing rows are left untouched. Sample appointments seed only on a
        // first, empty run, so a restart never duplicates them.
        List<Doctor> newDoctors = seedDoctors(specs);
        List<Patient> newPatients = seedPatients();

        if (appointmentRepository.count() == 0 && !newDoctors.isEmpty() && !newPatients.isEmpty()) {
            seedAppointments(newDoctors, newPatients);
        }

        log.warn("""

                =====================================================================
                 SAMPLE DATA LOADED
                 Patients : aarav.gupta@email.com, priya.sharma@email.com,
                            rahul.verma@email.com          password: {}
                 Doctors  : aditya.nair@medibridge.com, rohan.mehta@medibridge.com,
                            meera.joshi@medibridge.com     password: {}
                 Admin    : admin@medibridge.com           password: Admin@123
                 Swagger  : http://localhost:8080/api/swagger-ui.html
                =====================================================================""",
                DEMO_PASSWORD, DEMO_PASSWORD);
    }

    // --------------------------------------------------------------- doctors

    private List<Doctor> seedDoctors(Map<String, Specialization> specs) {
        record Seed(String name, String email, String phone, String spec, String licence,
                    int years, int fee, int duration, String bio, boolean active) {
        }

        List<Seed> seeds = List.of(
                new Seed("Dr. Aditya Nair", "aditya.nair@medibridge.com", "+91 98765 43210",
                        "Cardiology", "MD-12345-2020", 15, 800, 30,
                        "Interventional cardiologist. Special interest in preventive "
                        + "cardiology, hypertension and post-MI rehabilitation.", true),
                new Seed("Dr. Kavya Reddy", "kavya.reddy@medibridge.com", "+91 98765 43216",
                        "Cardiology", "MD-12351-2016", 12, 750, 30,
                        "Cardiologist focused on heart-failure management, echocardiography "
                        + "and women's cardiac health.", true),
                new Seed("Dr. Sameer Khan", "sameer.khan@medibridge.com", "+91 98765 43217",
                        "Cardiology", "MD-12352-2008", 20, 1100, 30,
                        "Senior interventional cardiologist with two decades in angioplasty "
                        + "and complex coronary care.", true),

                new Seed("Dr. Rohan Mehta", "rohan.mehta@medibridge.com", "+91 98765 43211",
                        "Dermatology", "MD-12346-2019", 12, 600, 20,
                        "Board-certified dermatologist treating acne, eczema, psoriasis "
                        + "and hair loss.", true),
                new Seed("Dr. Neha Kulkarni", "neha.kulkarni@medibridge.com", "+91 98765 43218",
                        "Dermatology", "MD-12353-2017", 9, 650, 20,
                        "Cosmetic and clinical dermatology - pigmentation, anti-ageing and "
                        + "laser treatments.", true),
                new Seed("Dr. Arjun Malhotra", "arjun.malhotra@medibridge.com", "+91 98765 43219",
                        "Dermatology", "MD-12354-2013", 14, 700, 20,
                        "Dermatologist and trichologist specialising in hair restoration "
                        + "and chronic skin conditions.", true),

                new Seed("Dr. Meera Joshi", "meera.joshi@medibridge.com", "+91 98765 43212",
                        "General Physician", "MD-12347-2021", 8, 500, 15,
                        "Family medicine. First point of contact for fever, infections, "
                        + "diabetes and routine health checks.", true),
                new Seed("Dr. Pooja Nair", "pooja.nair@medibridge.com", "+91 98765 43220",
                        "General Physician", "MD-12355-2019", 7, 450, 15,
                        "General physician with a focus on preventive care, thyroid and "
                        + "lifestyle disorders.", true),
                new Seed("Dr. Sanjay Gupta", "sanjay.gupta@medibridge.com", "+91 98765 43221",
                        "General Physician", "MD-12356-2010", 16, 550, 15,
                        "Internal medicine consultant managing diabetes, hypertension and "
                        + "seasonal illness.", true),

                new Seed("Dr. Rajesh Patel", "rajesh.patel@medibridge.com", "+91 98765 43213",
                        "Orthopedics", "MD-12348-2015", 18, 900, 30,
                        "Orthopaedic surgeon specialising in joint replacement, sports "
                        + "injuries and spine care.", true),
                new Seed("Dr. Deepak Sharma", "deepak.sharma@medibridge.com", "+91 98765 43222",
                        "Orthopedics", "MD-12357-2014", 13, 850, 30,
                        "Sports-medicine orthopaedist treating ligament injuries, arthroscopy "
                        + "and rehabilitation.", true),

                new Seed("Dr. Anita Desai", "anita.desai@medibridge.com", "+91 98765 43214",
                        "Pediatrics", "MD-12349-2018", 11, 550, 20,
                        "Paediatrician covering newborn care, vaccination schedules and "
                        + "childhood nutrition.", true),
                new Seed("Dr. Ritu Agarwal", "ritu.agarwal@medibridge.com", "+91 98765 43223",
                        "Pediatrics", "MD-12358-2017", 10, 500, 20,
                        "Paediatrician with interest in childhood asthma, allergies and "
                        + "developmental care.", true),

                new Seed("Dr. Nikhil Menon", "nikhil.menon@medibridge.com", "+91 98765 43224",
                        "Neurology", "MD-12359-2015", 11, 950, 30,
                        "Neurologist treating migraine, epilepsy, movement disorders and "
                        + "post-stroke care.", true),
                // Deliberately left pending so the admin approval screen has
                // something real to act on during a demo.
                new Seed("Dr. Vikram Rao", "vikram.rao@medibridge.com", "+91 98765 43215",
                        "Neurology", "MD-12350-2022", 6, 1000, 30,
                        "Neurologist with interest in migraine, epilepsy and stroke "
                        + "rehabilitation.", false));

        // Per-doctor idempotency: skip any email already in the database so a
        // restart adds only the doctors that are new.
        Set<String> existing = doctorRepository.findAll().stream()
                .map(d -> d.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        List<Doctor> saved = new ArrayList<>();

        for (Seed s : seeds) {
            if (existing.contains(s.email().toLowerCase())) {
                continue;
            }
            Specialization specialization = specs.get(s.spec());
            if (specialization == null) {
                log.warn("Skipping {} - specialization '{}' not found", s.name(), s.spec());
                continue;
            }

            Doctor doctor = doctorRepository.save(Doctor.builder()
                    .fullName(s.name())
                    .email(s.email())
                    .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                    .phone(s.phone())
                    .specialization(specialization)
                    .licenseNumber(s.licence())
                    .experienceYears(s.years())
                    .consultationFee(BigDecimal.valueOf(s.fee()))
                    .consultationDurationMin(s.duration())
                    .bio(s.bio())
                    .status(s.active() ? AccountStatus.ACTIVE : AccountStatus.PENDING)
                    .ratingAvg(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build());

            if (s.active()) {
                seedAvailability(doctor);
                seedSlots(doctor);
            }
            saved.add(doctor);
        }

        log.info("Seeded {} new doctors ({} total)", saved.size(), doctorRepository.count());
        return saved;
    }

    /** Mon-Fri mornings, with afternoons on alternating days. */
    private void seedAvailability(Doctor doctor) {
        DayOfWeek[] weekdays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        for (int i = 0; i < weekdays.length; i++) {
            availabilityRepository.save(DoctorAvailability.builder()
                    .doctor(doctor)
                    .dayOfWeek(weekdays[i])
                    .isAvailable(true)
                    .morning(true)
                    .afternoon(i % 2 == 0)
                    .build());
        }
    }

    /** Concrete bookable slots for the next 21 days. */
    private void seedSlots(Doctor doctor) {
        LocalDate today = LocalDate.now();
        int duration = doctor.getConsultationDurationMin();

        for (int day = 0; day < 21; day++) {
            LocalDate date = today.plusDays(day);
            DayOfWeek dow = date.getDayOfWeek();

            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }

            boolean afternoon = (dow.getValue() - 1) % 2 == 0;
            addWindow(doctor, date, LocalTime.of(9, 0), LocalTime.of(12, 0), duration);
            if (afternoon) {
                addWindow(doctor, date, LocalTime.of(14, 0), LocalTime.of(17, 0), duration);
            }
        }
    }

    private void addWindow(Doctor doctor, LocalDate date, LocalTime from, LocalTime to,
                           int minutes) {
        LocalTime cursor = from;
        while (!cursor.plusMinutes(minutes).isAfter(to)) {
            scheduleRepository.save(DoctorSchedule.builder()
                    .doctor(doctor)
                    .availableDate(date)
                    .startTime(cursor)
                    .endTime(cursor.plusMinutes(minutes))
                    .isBooked(false)
                    .build());
            cursor = cursor.plusMinutes(minutes);
        }
    }

    // -------------------------------------------------------------- patients

    private List<Patient> seedPatients() {
        record Seed(String name, String email, String phone, LocalDate dob,
                    Patient.Gender gender, String blood, String address) {
        }

        List<Seed> seeds = List.of(
                new Seed("Aarav Gupta", "aarav.gupta@email.com", "+91 90000 11111",
                        LocalDate.of(1990, 1, 15), Patient.Gender.Male, "O+",
                        "12 Baner Road, Pune, Maharashtra"),
                new Seed("Priya Sharma", "priya.sharma@email.com", "+91 90000 22222",
                        LocalDate.of(1995, 6, 22), Patient.Gender.Female, "A+",
                        "45 Koregaon Park, Pune, Maharashtra"),
                new Seed("Rahul Verma", "rahul.verma@email.com", "+91 90000 33333",
                        LocalDate.of(1982, 11, 3), Patient.Gender.Male, "B+",
                        "7 Andheri West, Mumbai, Maharashtra"),
                new Seed("Sneha Iyer", "sneha.iyer@email.com", "+91 90000 44444",
                        LocalDate.of(2001, 3, 28), Patient.Gender.Female, "AB+",
                        "88 Indiranagar, Bengaluru, Karnataka"));

        Set<String> existing = patientRepository.findAll().stream()
                .map(p -> p.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        List<Patient> saved = new ArrayList<>();
        for (Seed s : seeds) {
            if (existing.contains(s.email().toLowerCase())) {
                continue;
            }
            saved.add(patientRepository.save(Patient.builder()
                    .fullName(s.name())
                    .email(s.email())
                    .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                    .authProvider(Patient.AuthProvider.LOCAL)
                    .phone(s.phone())
                    .dateOfBirth(s.dob())
                    .gender(s.gender())
                    .bloodGroup(s.blood())
                    .address(s.address())
                    .status(AccountStatus.ACTIVE)
                    .build()));
        }

        log.info("Seeded {} new patients", saved.size());
        return saved;
    }

    // ---------------------------------------------------------- appointments

    /**
     * A spread of states so every screen has something to show: completed
     * consultations with prescriptions and reviews, a confirmed upcoming
     * appointment, and a cancelled one.
     */
    private void seedAppointments(List<Doctor> doctors, List<Patient> patients) {
        if (doctors.isEmpty() || patients.isEmpty()) {
            return;
        }

        // Pick by specialty, not list position - the doctor seed order must be
        // free to change without silently pairing an acne diagnosis with a
        // cardiologist.
        Doctor cardiologist = firstActiveBySpec(doctors, "Cardiology");
        Doctor dermatologist = firstActiveBySpec(doctors, "Dermatology");
        Doctor physician = firstActiveBySpec(doctors, "General Physician");
        if (cardiologist == null || dermatologist == null || physician == null) {
            return;
        }

        Patient john = patients.get(0);
        Patient priya = patients.get(1);
        Patient rahul = patients.get(2);

        // --- completed, with prescription and review ---
        Appointment a1 = completed(john, cardiologist, LocalDateTime.now().minusDays(12)
                .withHour(10).withMinute(0), "Chest tightness on exertion");
        prescribe(a1, "Hypertension, Stage 1",
                "BP 148/94 on two readings. ECG normal. Advised low-sodium diet.",
                "Reduce salt, 30 minutes brisk walking daily, monitor BP twice weekly.",
                List.of(
                        new String[]{"Amlodipine", "5 mg", "1-0-0", "30 days", "After breakfast"},
                        new String[]{"Telmisartan", "40 mg", "0-0-1", "30 days", "At bedtime"}));
        review(a1, (short) 5, Rating.OverallExperience.Excellent,
                Set.of(Rating.Highlight.CLEAR_EXPLANATIONS, Rating.Highlight.BEDSIDE_MANNER,
                        Rating.Highlight.ACCURATE_DIAGNOSIS),
                "The doctor explained my reports clearly and did not rush the consultation.");

        Appointment a2 = completed(priya, dermatologist, LocalDateTime.now().minusDays(6)
                .withHour(11).withMinute(0), "Persistent facial acne");
        prescribe(a2, "Acne vulgaris, moderate",
                "Inflammatory lesions over cheeks and jawline. No scarring.",
                "Wash twice daily with a gentle cleanser. Avoid picking lesions.",
                List.of(
                        new String[]{"Adapalene Gel 0.1%", "Topical", "0-0-1", "8 weeks",
                                "Apply thinly at night"},
                        new String[]{"Doxycycline", "100 mg", "1-0-0", "14 days", "After food"}));
        review(a2, (short) 4, Rating.OverallExperience.Good,
                Set.of(Rating.Highlight.FOLLOW_UP_CARE),
                "Helpful consultation, treatment is working well so far.");

        // --- completed, awaiting the patient's review ---
        completed(rahul, physician, LocalDateTime.now().minusDays(2)
                .withHour(9).withMinute(30), "Fever and sore throat for three days");

        // --- confirmed and upcoming ---
        confirmedUpcoming(john, physician, "Follow-up on blood pressure medication");

        // --- cancelled by the patient ---
        cancelled(priya, cardiologist, LocalDateTime.now().plusDays(9)
                .withHour(15).withMinute(0), "Schedule conflict at work");

        log.info("Seeded sample appointments, prescriptions and reviews");
    }

    private Appointment completed(Patient patient, Doctor doctor, LocalDateTime when,
                                  String reason) {
        BigDecimal fee = doctor.getConsultationFee();
        BigDecimal platform = BigDecimal.valueOf(5);

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient).doctor(doctor)
                .appointmentDate(when.withSecond(0).withNano(0))
                .status(AppointmentStatus.COMPLETED)
                .consultType("Consultation")
                .reason(reason)
                .bookedFee(fee).platformFee(platform).totalAmount(fee.add(platform))
                .confirmedAt(when.minusDays(1))
                .completedAt(when.plusMinutes(25))
                .build());

        payFor(appointment);
        return appointment;
    }

    private void confirmedUpcoming(Patient patient, Doctor doctor, String reason) {
        DoctorSchedule slot = scheduleRepository
                .findByDoctorIdAndAvailableDateOrderByStartTime(
                        doctor.getId(), nextWeekday(3))
                .stream().filter(s -> !Boolean.TRUE.equals(s.getIsBooked()))
                .findFirst().orElse(null);

        if (slot == null) {
            return;
        }

        LocalDateTime when = LocalDateTime.of(slot.getAvailableDate(), slot.getStartTime());
        BigDecimal fee = doctor.getConsultationFee();
        BigDecimal platform = BigDecimal.valueOf(5);

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient).doctor(doctor).schedule(slot)
                .appointmentDate(when)
                .status(AppointmentStatus.ACCEPTED)
                .consultType("Follow-up")
                .reason(reason)
                .bookedFee(fee).platformFee(platform).totalAmount(fee.add(platform))
                .confirmedAt(LocalDateTime.now().minusDays(1))
                .meetingLink("https://meet.jit.si/medibridge-"
                        + UUID.randomUUID().toString().replace("-", ""))
                .meetingSentAt(LocalDateTime.now().minusDays(1))
                .meetingJoinFrom(when.minusMinutes(15))
                .meetingValidUntil(when.plusMinutes(60))
                .build());

        slot.setIsBooked(true);
        scheduleRepository.save(slot);
        payFor(appointment);
    }

    private void cancelled(Patient patient, Doctor doctor, LocalDateTime when, String reason) {
        BigDecimal fee = doctor.getConsultationFee();
        BigDecimal platform = BigDecimal.valueOf(5);

        appointmentRepository.save(Appointment.builder()
                .patient(patient).doctor(doctor)
                .appointmentDate(when.withSecond(0).withNano(0))
                .status(AppointmentStatus.CANCELLED)
                .consultType("Consultation")
                .reason("Routine cardiac check")
                .bookedFee(fee).platformFee(platform).totalAmount(fee.add(platform))
                .cancelledAt(LocalDateTime.now().minusDays(1))
                .cancelledBy(Appointment.ActorRole.PATIENT)
                .cancellationReason(reason)
                .build());
    }

    private void payFor(Appointment appointment) {
        paymentRepository.save(PaymentTransaction.builder()
                .appointment(appointment)
                .amount(appointment.getTotalAmount())
                .consultationAmount(appointment.getBookedFee())
                .platformFee(appointment.getPlatformFee())
                .paymentMethod("UPI")
                .gateway(PaymentTransaction.Gateway.SIMULATED)
                .transactionRef("MB-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 16).toUpperCase())
                .transactionStatus(PaymentStatus.PAID)
                .build());
    }

    private void prescribe(Appointment appointment, String diagnosis, String notes,
                           String advice, List<String[]> medicines) {

        ConsultationRecord consultation = consultationRepository.save(
                ConsultationRecord.builder()
                        .appointment(appointment)
                        .diagnosis(diagnosis)
                        .notes(notes)
                        .followUpDate(appointment.getAppointmentDate().toLocalDate().plusDays(30))
                        .build());

        Prescription prescription = Prescription.builder()
                .consultation(consultation)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .dateIssued(appointment.getAppointmentDate().toLocalDate())
                .advice(advice)
                .items(new ArrayList<>())
                .build();

        int order = 0;
        for (String[] m : medicines) {
            prescription.addItem(PrescriptionItem.builder()
                    .medicineName(m[0]).dosage(m[1]).frequency(m[2])
                    .duration(m[3]).instructions(m[4]).sortOrder(order++)
                    .build());
        }

        prescriptionRepository.save(prescription);
    }

    private void review(Appointment appointment, short stars,
                        Rating.OverallExperience experience,
                        Set<Rating.Highlight> highlights, String text) {

        ratingRepository.save(Rating.builder()
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .stars(stars)
                .overallExperience(experience)
                .highlights(new LinkedHashSet<>(highlights))
                .reviewText(text)
                .build());

        // Keep the denormalised average on the doctor row consistent with the
        // reviews just inserted, exactly as ReviewService would.
        Doctor doctor = appointment.getDoctor();
        Double avg = ratingRepository.averageStarsForDoctor(doctor.getId());
        doctor.setRatingAvg(avg == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
        doctor.setRatingCount((int) ratingRepository.countByDoctorId(doctor.getId()));
        doctorRepository.save(doctor);
    }

    private Doctor firstActiveBySpec(List<Doctor> doctors, String specName) {
        return doctors.stream()
                .filter(d -> d.getStatus() == AccountStatus.ACTIVE
                        && specName.equals(d.getSpecialization().getName()))
                .findFirst()
                .orElse(null);
    }

    private LocalDate nextWeekday(int daysAhead) {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
