package com.medibridge.patient;

import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.RecordService;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Family / dependent profiles.
 *
 * <p>The interesting question is not "can a parent book for a child" - it is
 * whether one account can reach another account's dependent. That is asserted at
 * both levels the design claims to defend: the service returns 404, and the
 * composite foreign key refuses the row even when the service is bypassed
 * entirely. The second assertion is the one that matters, because it is the only
 * one that survives someone adding a new write path later.
 */
class FamilyProfilesTest extends AbstractIntegrationTest {

    @Autowired RecordService recordService;
    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------- ownership

    @Test
    @DisplayName("booking against another account's dependent is 404, not 403")
    void cannotBookForSomeoneElsesDependent() {
        Patient owner = newPatient();
        Patient attacker = newPatient();
        FamilyMember theirChild = newDependent(owner);
        Doctor doctor = newDoctor();

        assertThatThrownBy(() -> appointmentService.book(attacker.getId(),
                new BookAppointmentRequest(doctor.getId(), slotInDays(doctor, 3).getId(),
                        "Consultation", "Fever", theirChild.getId())))
                .isInstanceOf(ResourceNotFoundException.class)
                // 403 would confirm the id exists and let the range be walked to
                // count other people's families.
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("the database refuses a cross-account dependent even without the service")
    void compositeForeignKeyRefusesCrossAccountDependent() {
        Patient owner = newPatient();
        Patient other = newPatient();
        FamilyMember ownersChild = newDependent(owner);
        Doctor doctor = newDoctor();

        // Straight past every Java check, the way a future endpoint or a manual
        // fix-up script would. (family_member_id, patient_id) has to exist as a
        // pair in family_member, and this pair never will.
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO appointment
                          (patient_id, family_member_id, doctor_id, appointment_date,
                           status, consult_type, reschedule_count)
                        VALUES (?, ?, ?, ?, 'PendingPayment', 'Consultation', 0)
                        """,
                other.getId(), ownersChild.getId(), doctor.getId(),
                LocalDateTime.now().plusDays(4)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a removed dependent can no longer be booked for")
    void archivedDependentCannotBeBooked() {
        Patient owner = newPatient();
        FamilyMember child = newDependent(owner);
        Doctor doctor = newDoctor();

        familyMemberService.archive(owner.getId(), child.getId());

        assertThat(familyMemberService.list(owner.getId()))
                .extracting("familyMemberId").doesNotContain(child.getId());

        assertThatThrownBy(() -> appointmentService.book(owner.getId(),
                new BookAppointmentRequest(doctor.getId(), slotInDays(doctor, 3).getId(),
                        "Consultation", "Fever", child.getId())))
                .isInstanceOf(BadRequestException.class);
    }

    // ------------------------------------------------------------ the payload

    @Test
    @DisplayName("the visit carries the child's name and age, not the parent's")
    void appointmentReportsTheSubjectNotThePayer() {
        Patient parent = newPatient();
        FamilyMember child = newDependent(parent, "Aarav Sharma",
                FamilyMember.Relation.Child, 8);
        Doctor doctor = newDoctor();

        var booked = appointmentService.book(parent.getId(), new BookAppointmentRequest(
                doctor.getId(), slotInDays(doctor, 3).getId(), "Consultation",
                "Persistent cough", child.getId()));

        // The name a paediatrician's list must show - showing the parent here is
        // what sends the wrong person into the room.
        assertThat(booked.patient()).isEqualTo("Aarav Sharma");
        assertThat(booked.age()).isEqualTo(8);
        assertThat(booked.familyMemberId()).isEqualTo(child.getId());
        assertThat(booked.bookedFor()).isEqualTo("Child");
        assertThat(booked.accountHolder()).isEqualTo(parent.getFullName());

        // The account holder is still the payer and the canceller.
        assertThat(booked.patientId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("a self-booking sends no dependent fields at all")
    void selfBookingIsUnchanged() {
        Patient patient = newPatient();
        Doctor doctor = newDoctor();

        var booked = appointmentService.book(patient.getId(), new BookAppointmentRequest(
                doctor.getId(), slotInDays(doctor, 3).getId(), "Consultation", "Fever", null));

        assertThat(booked.patient()).isEqualTo(patient.getFullName());
        assertThat(booked.familyMemberId()).isNull();
        assertThat(booked.bookedFor()).isNull();
        assertThat(booked.accountHolder()).isNull();
    }

    // --------------------------------------------------------------- records

    @Test
    @DisplayName("a dependent's report is the owner's to read and nobody else's")
    void dependentRecordsBelongToTheOwningAccount() {
        Patient owner = newPatient();
        Patient stranger = newPatient();
        FamilyMember child = newDependent(owner);

        var uploaded = recordService.upload(owner.getId(), child.getId(),
                pdf("child-xray.pdf"), "Chest X-Ray", "Radiology");

        assertThat(uploaded.familyMemberId()).isEqualTo(child.getId());
        assertThat(uploaded.patientName()).isEqualTo(child.getFullName());

        // The owner sees it in both the combined list and the child's own list.
        assertThat(recordService.listForPatient(owner.getId()))
                .extracting("reportId").contains(uploaded.reportId());
        assertThat(recordService.listForSubject(owner.getId(), String.valueOf(child.getId())))
                .extracting("reportId").containsExactly(uploaded.reportId());

        // ...but not in their own, which is what the second-opinion rule counts.
        assertThat(recordService.listForSubject(owner.getId(), RecordService.SUBJECT_SELF))
                .isEmpty();
        assertThat(recordService.countForSubject(owner.getId(), null)).isZero();
        assertThat(recordService.countForSubject(owner.getId(), child.getId())).isEqualTo(1);

        // A stranger gets 404 on the file and 404 on the dependent id itself.
        assertThatThrownBy(() ->
                recordService.downloadAsPatient(stranger.getId(), uploaded.reportId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() ->
                recordService.listForSubject(stranger.getId(), String.valueOf(child.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("treating the child does not open the parent's file")
    void doctorSeesOnlyTheSubjectTheyTreated() {
        Patient parent = newPatient();
        FamilyMember child = newDependent(parent);
        Doctor paediatrician = newDoctor();

        var childReport = recordService.upload(parent.getId(), child.getId(),
                pdf("child.pdf"), "Child Bloodwork", "Lab Report");
        var parentReport = recordService.upload(parent.getId(), null,
                pdf("parent.pdf"), "Parent ECG", "Cardiology");

        // The only consultation this doctor has ever had is with the child.
        paidAppointment(parent, child, slotInDays(paediatrician, 2));

        assertThat(recordService.listForDoctorsPatient(paediatrician.getId(), parent.getId()))
                .extracting("reportId")
                .containsExactly(childReport.reportId());

        // Same login, same account - and still out of reach, because the match is
        // on who was treated, not on whose account paid.
        assertThatThrownBy(() -> recordService.downloadAsDoctor(
                paediatrician.getId(), parentReport.reportId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------- CRUD

    @Test
    @DisplayName("a dependent can only be edited or removed by their owner")
    void mutationsAreOwnerScoped() {
        Patient owner = newPatient();
        Patient attacker = newPatient();
        FamilyMember child = newDependent(owner);

        var request = new com.medibridge.patient.dto.FamilyMemberRequest(
                "Renamed", child.getDateOfBirth(), Patient.Gender.Female,
                FamilyMember.Relation.Child, "A+", "9999999999");

        assertThatThrownBy(() ->
                familyMemberService.update(attacker.getId(), child.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() ->
                familyMemberService.archive(attacker.getId(), child.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        var updated = familyMemberService.update(owner.getId(), child.getId(), request);
        assertThat(updated.fullName()).isEqualTo("Renamed");
        assertThat(updated.gender()).isEqualTo("Female");
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.4 fake".getBytes());
    }
}
