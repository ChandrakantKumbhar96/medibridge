package com.medibridge.patient;

import com.medibridge.patient.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

    /**
     * Ownership is baked into the query, not applied afterwards - there is
     * deliberately no {@code findById} in use anywhere for this entity, so a
     * caller cannot accidentally load someone else's dependent and then forget
     * to compare the owner.
     */
    Optional<FamilyMember> findByIdAndOwnerId(Integer id, Integer ownerId);

    List<FamilyMember> findByOwnerIdAndArchivedAtIsNullOrderByFullNameAsc(Integer ownerId);

    List<FamilyMember> findByOwnerIdOrderByFullNameAsc(Integer ownerId);
}
