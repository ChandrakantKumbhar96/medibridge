package com.medibridge.common.config;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.PaymentStatus;
import com.medibridge.common.util.PhoneNumbers;
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
    private static final String DEFAULT_LANGUAGES = "English, Hindi, Marathi";

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

        // Every active doctor should show real stars, not "New" - give any that
        // have no reviews yet a few completed consultations and ratings.
        seedRatingsForNewDoctors();

        // Pair this run's new patients 1:1 with this run's new doctors so the
        // extra accounts aren't empty shells - each pair gets an appointment,
        // and about half get a full consultation/prescription/review chain too.
        seedMoreDemoAppointments(newDoctors, newPatients);

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
                        + "rehabilitation.", false),

                new Seed("Dr. Farhan Ali", "farhan.ali@medibridge.com", "+91 98765 43225",
                        "Cardiology", "MD-12460-2011", 14, 850, 30,
                        "Cardiologist specialising in arrhythmia management and pacemaker "
                        + "follow-up.", true),
                new Seed("Dr. Ishita Bhatt", "ishita.bhatt@medibridge.com", "+91 98765 43226",
                        "Cardiology", "MD-12461-2019", 7, 700, 30,
                        "General cardiology with a focus on lipid disorders and cardiac "
                        + "rehabilitation.", true),
                new Seed("Dr. Manoj Trivedi", "manoj.trivedi@medibridge.com", "+91 98765 43227",
                        "Cardiology", "MD-12462-2006", 22, 1200, 30,
                        "Senior cardiologist experienced in heart failure and valvular "
                        + "disease.", true),
                new Seed("Dr. Ritika Chawla", "ritika.chawla@medibridge.com", "+91 98765 43228",
                        "Cardiology", "MD-12463-2015", 11, 780, 30,
                        "Preventive cardiology and echocardiography specialist.", true),

                new Seed("Dr. Aakash Verma", "aakash.verma@medibridge.com", "+91 98765 43229",
                        "Dermatology", "MD-12464-2016", 10, 620, 20,
                        "Dermatologist treating eczema, psoriasis and vitiligo.", true),
                new Seed("Dr. Simran Kaur", "simran.kaur@medibridge.com", "+91 98765 43230",
                        "Dermatology", "MD-12465-2012", 13, 680, 20,
                        "Cosmetic dermatologist specialising in chemical peels and laser "
                        + "resurfacing.", true),
                new Seed("Dr. Yash Choudhary", "yash.choudhary@medibridge.com", "+91 98765 43231",
                        "Dermatology", "MD-12466-2020", 6, 580, 20,
                        "Dermatologist focused on adolescent acne and scar management.", true),

                new Seed("Dr. Kiran Bose", "kiran.bose@medibridge.com", "+91 98765 43232",
                        "General Physician", "MD-12467-2009", 17, 520, 15,
                        "Internal medicine physician managing chronic illness and "
                        + "preventive screening.", true),
                new Seed("Dr. Aditi Rane", "aditi.rane@medibridge.com", "+91 98765 43233",
                        "General Physician", "MD-12468-2018", 8, 470, 15,
                        "Family physician for routine check-ups, infections and "
                        + "vaccinations.", true),
                new Seed("Dr. Suresh Iyer", "suresh.iyer@medibridge.com", "+91 98765 43234",
                        "General Physician", "MD-12469-2005", 20, 600, 15,
                        "Senior general physician with focus on geriatric and lifestyle "
                        + "care.", true),

                new Seed("Dr. Varun Kapoor", "varun.kapoor@medibridge.com", "+91 98765 43235",
                        "Orthopedics", "MD-12470-2013", 15, 880, 30,
                        "Orthopaedic surgeon specialising in knee and hip replacement.", true),
                new Seed("Dr. Nandini Rao", "nandini.rao@medibridge.com", "+91 98765 43236",
                        "Orthopedics", "MD-12471-2017", 9, 760, 30,
                        "Sports orthopaedist treating ligament and tendon injuries.", true),
                new Seed("Dr. Pranav Joshi", "pranav.joshi@medibridge.com", "+91 98765 43237",
                        "Orthopedics", "MD-12472-2010", 16, 900, 30,
                        "Spine specialist managing disc disorders and chronic back pain.", true),
                new Seed("Dr. Tanya Malik", "tanya.malik@medibridge.com", "+91 98765 43238",
                        "Orthopedics", "MD-12473-2021", 5, 700, 30,
                        "Orthopaedic surgeon with interest in fracture care and "
                        + "rehabilitation.", true),

                new Seed("Dr. Rakesh Suri", "rakesh.suri@medibridge.com", "+91 98765 43239",
                        "Pediatrics", "MD-12474-2014", 12, 530, 20,
                        "Paediatrician covering growth monitoring and childhood "
                        + "infections.", true),
                new Seed("Dr. Divya Menon", "divya.menon@medibridge.com", "+91 98765 43240",
                        "Pediatrics", "MD-12475-2019", 7, 490, 20,
                        "Paediatrician with special interest in neonatal care.", true),
                new Seed("Dr. Amit Saxena", "amit.saxena@medibridge.com", "+91 98765 43241",
                        "Pediatrics", "MD-12476-2008", 18, 560, 20,
                        "Senior paediatrician managing chronic childhood conditions and "
                        + "allergies.", true),

                new Seed("Dr. Shalini Pillai", "shalini.pillai@medibridge.com", "+91 98765 43242",
                        "Neurology", "MD-12477-2012", 13, 980, 30,
                        "Neurologist specialising in epilepsy and headache disorders.", true),
                new Seed("Dr. Gaurav Bansal", "gaurav.bansal@medibridge.com", "+91 98765 43243",
                        "Neurology", "MD-12478-2016", 9, 900, 30,
                        "Neurologist treating movement disorders and neuropathy.", true),
                new Seed("Dr. Preeti Nambiar", "preeti.nambiar@medibridge.com", "+91 98765 43244",
                        "Neurology", "MD-12479-2007", 19, 1050, 30,
                        "Senior neurologist with expertise in stroke care and "
                        + "rehabilitation.", true));

        // Per-doctor idempotency: an email already in the DB is not recreated,
        // but its qualifications/languages are backfilled if missing (those
        // columns were added later, in migration V7).
        Map<String, Doctor> existing = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getEmail().toLowerCase(), d -> d));

        List<Doctor> saved = new ArrayList<>();

        for (Seed s : seeds) {
            Doctor already = existing.get(s.email().toLowerCase());
            if (already != null) {
                backfillProfile(already);
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
                    .qualifications(qualificationsFor(s.spec()))
                    .languages(DEFAULT_LANGUAGES)
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

    /** Specialty-appropriate degrees, the way a real doctor profile reads. */
    private String qualificationsFor(String spec) {
        return switch (spec) {
            case "Cardiology" -> "MBBS, MD (Medicine), DM (Cardiology)";
            case "Dermatology" -> "MBBS, MD (Dermatology)";
            case "General Physician" -> "MBBS, MD (General Medicine)";
            case "Orthopedics" -> "MBBS, MS (Orthopaedics)";
            case "Pediatrics" -> "MBBS, MD (Paediatrics)";
            case "Neurology" -> "MBBS, MD, DM (Neurology)";
            default -> "MBBS";
        };
    }

    /** Fill qualifications/languages on a pre-existing doctor row (added by V7). */
    private void backfillProfile(Doctor doctor) {
        boolean changed = false;
        if (doctor.getQualifications() == null || doctor.getQualifications().isBlank()) {
            doctor.setQualifications(qualificationsFor(doctor.getSpecialization().getName()));
            changed = true;
        }
        if (doctor.getLanguages() == null || doctor.getLanguages().isBlank()) {
            doctor.setLanguages(DEFAULT_LANGUAGES);
            changed = true;
        }
        if (changed) {
            doctorRepository.save(doctor);
        }
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
                        "88 Indiranagar, Bengaluru, Karnataka"),
                new Seed("Karan Malhotra", "karan.malhotra@email.com", "+91 90000 55555",
                        LocalDate.of(1988, 7, 19), Patient.Gender.Male, "O-",
                        "23 Connaught Place, New Delhi"),
                new Seed("Ananya Rao", "ananya.rao@email.com", "+91 90000 66666",
                        LocalDate.of(1993, 2, 11), Patient.Gender.Female, "B-",
                        "56 Banjara Hills, Hyderabad, Telangana"),
                new Seed("Vivek Nair", "vivek.nair@email.com", "+91 90000 77777",
                        LocalDate.of(1979, 9, 5), Patient.Gender.Male, "A+",
                        "9 Marine Drive, Kochi, Kerala"),
                new Seed("Isha Kapoor", "isha.kapoor@email.com", "+91 90000 88888",
                        LocalDate.of(1998, 12, 30), Patient.Gender.Female, "AB-",
                        "34 Sector 17, Chandigarh"),
                new Seed("Rohit Deshmukh", "rohit.deshmukh@email.com", "+91 90000 99999",
                        LocalDate.of(1985, 4, 17), Patient.Gender.Male, "O+",
                        "78 Dharampeth, Nagpur, Maharashtra"),
                new Seed("Meghna Pillai", "meghna.pillai@email.com", "+91 90001 00000",
                        LocalDate.of(2000, 8, 8), Patient.Gender.Female, "A-",
                        "12 T. Nagar, Chennai, Tamil Nadu"),

                new Seed("Aditya Kulkarni", "aditya.kulkarni@email.com", "+91 90002 10001",
                        LocalDate.of(1992, 5, 14), Patient.Gender.Male, "B+",
                        "22 FC Road, Pune, Maharashtra"),
                new Seed("Nisha Agarwal", "nisha.agarwal@email.com", "+91 90002 10002",
                        LocalDate.of(1997, 9, 2), Patient.Gender.Female, "O+",
                        "14 Salt Lake, Kolkata, West Bengal"),
                new Seed("Devendra Singh", "devendra.singh@email.com", "+91 90002 10003",
                        LocalDate.of(1980, 1, 21), Patient.Gender.Male, "A-",
                        "5 Civil Lines, Lucknow, Uttar Pradesh"),
                new Seed("Pallavi Joshi", "pallavi.joshi@email.com", "+91 90002 10004",
                        LocalDate.of(1999, 11, 8), Patient.Gender.Female, "AB+",
                        "31 Vastrapur, Ahmedabad, Gujarat"),
                new Seed("Harsh Vardhan", "harsh.vardhan@email.com", "+91 90002 10005",
                        LocalDate.of(1986, 3, 30), Patient.Gender.Male, "O-",
                        "8 Sadar Bazaar, Jaipur, Rajasthan"),
                new Seed("Swati Bhosale", "swati.bhosale@email.com", "+91 90002 10006",
                        LocalDate.of(1994, 7, 17), Patient.Gender.Female, "B-",
                        "19 Kothrud, Pune, Maharashtra"),
                new Seed("Manish Tiwari", "manish.tiwari@email.com", "+91 90002 10007",
                        LocalDate.of(1978, 12, 25), Patient.Gender.Male, "A+",
                        "3 MP Nagar, Bhopal, Madhya Pradesh"),
                new Seed("Radhika Menon", "radhika.menon@email.com", "+91 90002 10008",
                        LocalDate.of(2002, 4, 19), Patient.Gender.Female, "O+",
                        "27 Vyttila, Kochi, Kerala"),
                new Seed("Sourav Chatterjee", "sourav.chatterjee@email.com", "+91 90002 10009",
                        LocalDate.of(1983, 6, 11), Patient.Gender.Male, "AB-",
                        "44 Park Street, Kolkata, West Bengal"),
                new Seed("Ankita Deshpande", "ankita.deshpande@email.com", "+91 90002 10010",
                        LocalDate.of(1991, 10, 5), Patient.Gender.Female, "A+",
                        "16 Deccan Gymkhana, Pune, Maharashtra"),
                new Seed("Vishal Reddy", "vishal.reddy@email.com", "+91 90002 10011",
                        LocalDate.of(1989, 2, 27), Patient.Gender.Male, "B+",
                        "60 Jubilee Hills, Hyderabad, Telangana"),
                new Seed("Neelam Yadav", "neelam.yadav@email.com", "+91 90002 10012",
                        LocalDate.of(1996, 8, 13), Patient.Gender.Female, "O-",
                        "9 Sector 62, Noida, Uttar Pradesh"),
                new Seed("Arvind Pandey", "arvind.pandey@email.com", "+91 90002 10013",
                        LocalDate.of(1975, 5, 9), Patient.Gender.Male, "A-",
                        "21 Alambagh, Lucknow, Uttar Pradesh"),
                new Seed("Bhavna Shah", "bhavna.shah@email.com", "+91 90002 10014",
                        LocalDate.of(2000, 1, 30), Patient.Gender.Female, "AB+",
                        "38 Satellite, Ahmedabad, Gujarat"),
                new Seed("Kunal Oberoi", "kunal.oberoi@email.com", "+91 90002 10015",
                        LocalDate.of(1987, 9, 22), Patient.Gender.Male, "O+",
                        "11 Model Town, New Delhi"),
                new Seed("Shreya Kulkarni", "shreya.kulkarni@email.com", "+91 90002 10016",
                        LocalDate.of(1993, 12, 3), Patient.Gender.Female, "B-",
                        "25 Aundh, Pune, Maharashtra"),
                new Seed("Tarun Bhatia", "tarun.bhatia@email.com", "+91 90002 10017",
                        LocalDate.of(1981, 4, 16), Patient.Gender.Male, "A+",
                        "17 Sector 15, Chandigarh"),
                new Seed("Ishika Kapoor", "ishika.kapoor@email.com", "+91 90002 10018",
                        LocalDate.of(1998, 6, 28), Patient.Gender.Female, "O+",
                        "48 Malviya Nagar, Jaipur, Rajasthan"),
                new Seed("Rajat Nanda", "rajat.nanda@email.com", "+91 90002 10019",
                        LocalDate.of(1984, 11, 14), Patient.Gender.Male, "AB-",
                        "6 Vashi, Navi Mumbai, Maharashtra"),
                new Seed("Meenal Kelkar", "meenal.kelkar@email.com", "+91 90002 10020",
                        LocalDate.of(2001, 2, 8), Patient.Gender.Female, "B+",
                        "33 Shivaji Nagar, Pune, Maharashtra"));

        // A phone-first account has no email at all (V14), and this ran at
        // startup - so one OTP signup made every subsequent boot fail here.
        Set<String> existing = patientRepository.findAll().stream()
                .map(Patient::getEmail)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
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
                    // Same normalisation V14 backfills with, so a demo patient
                    // can sign in by code as well as by password.
                    .phoneE164(PhoneNumbers.toE164(s.phone()))
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

    /**
     * Extra appointments for this run's newly seeded patients/doctors, pairing
     * them 1:1. Roughly half go through the full completed+prescribed+reviewed
     * chain, the rest are a mix of upcoming and cancelled so those screens have
     * more than a handful of rows to show too.
     */
    private void seedMoreDemoAppointments(List<Doctor> newDoctors, List<Patient> newPatients) {
        if (newDoctors.isEmpty() || newPatients.isEmpty()) {
            return;
        }

        int pairs = Math.min(newDoctors.size(), newPatients.size());
        for (int i = 0; i < pairs; i++) {
            Patient patient = newPatients.get(i);
            Doctor doctor = newDoctors.get(i);

            switch (i % 4) {
                case 0 -> cancelled(patient, doctor, LocalDateTime.now().plusDays(5 + i)
                        .withHour(11).withMinute(0), "Patient requested reschedule");
                case 1 -> confirmedUpcoming(patient, doctor, "Routine consultation");
                default -> {
                    Appointment appt = completed(patient, doctor, LocalDateTime.now()
                            .minusDays(1 + i).withHour(9 + (i % 6)).withMinute(0),
                            "Consultation with " + doctor.getFullName());
                    DemoPrescription rx = prescriptionFor(doctor.getSpecialization().getName());
                    prescribe(appt, rx.diagnosis(), rx.notes(), rx.advice(), rx.medicines());
                    review(appt, rx.stars(), rx.experience(), rx.highlights(), rx.reviewText());
                }
            }
        }

        log.info("Seeded {} additional demo appointments", pairs);
    }

    private record DemoPrescription(String diagnosis, String notes, String advice,
                                    List<String[]> medicines, short stars,
                                    Rating.OverallExperience experience,
                                    Set<Rating.Highlight> highlights, String reviewText) {
    }

    /** Specialty-appropriate demo diagnosis/prescription/review, reused across pairs. */
    private DemoPrescription prescriptionFor(String spec) {
        return switch (spec) {
            case "Cardiology" -> new DemoPrescription(
                    "Hypertension, Stage 1", "BP mildly elevated on repeat readings. ECG normal.",
                    "Low-sodium diet, regular light exercise, monitor BP weekly.",
                    List.<String[]>of(new String[]{"Amlodipine", "5 mg", "1-0-0", "30 days", "After breakfast"}),
                    (short) 5, Rating.OverallExperience.Excellent,
                    Set.of(Rating.Highlight.CLEAR_EXPLANATIONS, Rating.Highlight.ACCURATE_DIAGNOSIS),
                    "Thorough check-up, explained the readings clearly.");
            case "Dermatology" -> new DemoPrescription(
                    "Acne vulgaris, mild", "Scattered inflammatory lesions, no scarring.",
                    "Gentle cleanser twice daily, avoid picking lesions.",
                    List.<String[]>of(new String[]{"Adapalene Gel 0.1%", "Topical", "0-0-1", "8 weeks",
                            "Apply thinly at night"}),
                    (short) 4, Rating.OverallExperience.Good,
                    Set.of(Rating.Highlight.FOLLOW_UP_CARE),
                    "Skin is already clearing up after a couple of weeks.");
            case "Orthopedics" -> new DemoPrescription(
                    "Mechanical lower back pain", "No radicular signs. Reduced lumbar flexion.",
                    "Physiotherapy twice weekly, avoid heavy lifting for 2 weeks.",
                    List.<String[]>of(new String[]{"Aceclofenac", "100 mg", "1-0-1", "5 days", "After food"}),
                    (short) 4, Rating.OverallExperience.Good,
                    Set.of(Rating.Highlight.BEDSIDE_MANNER),
                    "Pain has reduced a lot after following the advice.");
            case "Pediatrics" -> new DemoPrescription(
                    "Viral upper respiratory infection", "Mild fever, clear chest on auscultation.",
                    "Plenty of fluids, rest, paracetamol only if fever crosses 100.4F.",
                    List.<String[]>of(new String[]{"Paracetamol Syrup", "125 mg/5ml", "SOS", "5 days",
                            "Only if fever"}),
                    (short) 5, Rating.OverallExperience.Excellent,
                    Set.of(Rating.Highlight.FRIENDLY_STAFF, Rating.Highlight.BEDSIDE_MANNER),
                    "Great with kids, made my child comfortable throughout.");
            case "Neurology" -> new DemoPrescription(
                    "Migraine without aura", "Episodic, triggered by stress and poor sleep.",
                    "Maintain a headache diary, regular sleep schedule, avoid known triggers.",
                    List.<String[]>of(new String[]{"Rizatriptan", "10 mg", "SOS", "30 days",
                            "At onset of headache"}),
                    (short) 5, Rating.OverallExperience.Excellent,
                    Set.of(Rating.Highlight.ACCURATE_DIAGNOSIS, Rating.Highlight.FOLLOW_UP_CARE),
                    "Finally got a clear explanation for what triggers my migraines.");
            default -> new DemoPrescription(
                    "General wellness check-up", "No acute findings on examination.",
                    "Maintain a balanced diet and routine annual check-ups.",
                    List.<String[]>of(new String[]{"Multivitamin", "1 tablet", "1-0-0", "30 days",
                            "After breakfast"}),
                    (short) 4, Rating.OverallExperience.Good,
                    Set.of(Rating.Highlight.FRIENDLY_STAFF),
                    "Smooth consultation, no complaints.");
        };
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

    /**
     * Give every active doctor without reviews a few completed consultations and
     * ratings, so listing and profile pages show real stars rather than "New".
     * Idempotent: a doctor that already has ratings is skipped.
     */
    private void seedRatingsForNewDoctors() {
        List<Patient> patients = patientRepository.findAll();
        if (patients.isEmpty()) {
            return;
        }

        List<Doctor> unrated = doctorRepository.findAll().stream()
                .filter(d -> d.getStatus() == AccountStatus.ACTIVE)
                .filter(d -> d.getRatingCount() == null || d.getRatingCount() == 0)
                .toList();

        record R(short stars, Rating.OverallExperience exp,
                 Set<Rating.Highlight> tags, String text) {
        }
        List<R> samples = List.of(
                new R((short) 5, Rating.OverallExperience.Excellent,
                        Set.of(Rating.Highlight.CLEAR_EXPLANATIONS, Rating.Highlight.BEDSIDE_MANNER),
                        "Very patient and thorough - explained my condition in simple terms."),
                new R((short) 5, Rating.OverallExperience.Excellent,
                        Set.of(Rating.Highlight.ACCURATE_DIAGNOSIS, Rating.Highlight.FOLLOW_UP_CARE),
                        "Spot-on diagnosis and a clear follow-up plan. Highly recommend."),
                new R((short) 4, Rating.OverallExperience.Good,
                        Set.of(Rating.Highlight.FRIENDLY_STAFF),
                        "Good consultation, the prescription is working well so far."));

        int p = 0;
        int seeded = 0;
        for (Doctor doctor : unrated) {
            int count = 2 + (seeded % 2);   // 2 or 3 reviews per doctor
            for (int i = 0; i < count; i++) {
                Patient patient = patients.get(p++ % patients.size());
                Appointment appointment = completed(patient, doctor,
                        LocalDateTime.now().minusDays(4L + i).withHour(10 + i).withMinute(0),
                        "Consultation");
                R r = samples.get(i % samples.size());
                review(appointment, r.stars(), r.exp(), r.tags(), r.text());
            }
            seeded++;
        }

        if (seeded > 0) {
            log.info("Seeded reviews for {} previously-unrated doctors", seeded);
        }
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
