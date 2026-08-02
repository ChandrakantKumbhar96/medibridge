package com.medibridge.patient;

import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.patient.dto.FamilyMemberRequest;
import com.medibridge.patient.dto.FamilyMemberResponse;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dependent profiles owned by an account holder.
 *
 * <p>Every method takes the owner's id first and every lookup is by
 * {@code (id, ownerId)}. A dependent is never loaded by its own id, so there is
 * no code path where an ownership comparison could be forgotten.
 */
@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * A ceiling, not a business rule. Nothing legitimate needs dozens of
     * dependents, and without a cap this is an unauthenticated-shaped way for one
     * account to grow the table without bound.
     */
    private static final int MAX_ACTIVE_DEPENDENTS = 8;

    private final FamilyMemberRepository familyMemberRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> list(Integer ownerId) {
        return familyMemberRepository
                .findByOwnerIdAndArchivedAtIsNullOrderByFullNameAsc(ownerId)
                .stream().map(FamilyMemberService::toDto).toList();
    }

    @Transactional
    public FamilyMemberResponse create(Integer ownerId, FamilyMemberRequest request) {
        Patient owner = patientRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        long active = familyMemberRepository
                .findByOwnerIdAndArchivedAtIsNullOrderByFullNameAsc(ownerId).size();
        if (active >= MAX_ACTIVE_DEPENDENTS) {
            throw new BadRequestException(
                    "You can have at most " + MAX_ACTIVE_DEPENDENTS + " family profiles. "
                            + "Remove one you no longer book for to add another.");
        }

        FamilyMember member = FamilyMember.builder()
                .owner(owner)
                .fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .relation(request.relation())
                .bloodGroup(blankToNull(request.bloodGroup()))
                .phone(blankToNull(request.phone()))
                .build();

        return toDto(familyMemberRepository.save(member));
    }

    @Transactional
    public FamilyMemberResponse update(Integer ownerId, Integer familyMemberId,
                                       FamilyMemberRequest request) {
        FamilyMember member = requireOwned(ownerId, familyMemberId);

        member.setFullName(request.fullName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setRelation(request.relation());
        member.setBloodGroup(blankToNull(request.bloodGroup()));
        member.setPhone(blankToNull(request.phone()));

        return toDto(familyMemberRepository.save(member));
    }

    /**
     * Archives rather than deletes.
     *
     * <p>The appointment and report foreign keys are {@code ON DELETE RESTRICT},
     * so a dependent who has ever been booked for cannot be removed anyway - and
     * should not be. Archiving takes them out of the booking dropdown while their
     * history stays readable to the owner.
     */
    @Transactional
    public void archive(Integer ownerId, Integer familyMemberId) {
        FamilyMember member = requireOwned(ownerId, familyMemberId);
        if (!member.isArchived()) {
            member.setArchivedAt(LocalDateTime.now());
            familyMemberRepository.save(member);
        }
    }

    /**
     * The module boundary for everyone else.
     *
     * <p>The appointment and record modules need the entity to hang a JPA
     * relation off, but they must not reach into {@code FamilyMemberRepository} -
     * same rule that put {@code RecordService.countForSubject} where it is. They
     * ask here, and get back either a dependent this owner really owns or a 404.
     *
     * <p>404, not 403: a 403 would confirm the id exists and let someone walk the
     * range to count other people's families.
     *
     * @return the dependent, or null when {@code familyMemberId} is null - which
     *         is the ordinary "booking for myself" case, not an error.
     */
    @Transactional(readOnly = true)
    public FamilyMember requireOwned(Integer ownerId, Integer familyMemberId) {
        if (familyMemberId == null) {
            return null;
        }
        FamilyMember member = familyMemberRepository
                .findByIdAndOwnerId(familyMemberId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));

        // Archived profiles keep their history but must not attract new bookings
        // or uploads, or "remove" would be a button that does nothing.
        if (member.isArchived()) {
            throw new BadRequestException(
                    member.getFullName() + " has been removed from your family profiles.");
        }
        return member;
    }

    // ----------------------------------------------------------------- mapping

    public static FamilyMemberResponse toDto(FamilyMember m) {
        return new FamilyMemberResponse(
                m.getId(),
                m.getFullName(),
                m.getDateOfBirth().format(DATE),
                ageOf(m.getDateOfBirth()),
                m.getGender().name(),
                m.getRelation().name(),
                m.getBloodGroup(),
                m.getPhone(),
                m.isArchived() ? Boolean.TRUE : null);
    }

    public static Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
