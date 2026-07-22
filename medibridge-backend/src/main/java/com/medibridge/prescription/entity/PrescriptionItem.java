package com.medibridge.prescription.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    /** e.g. "500 mg" */
    @Column(nullable = false, length = 50)
    private String dosage;

    /** e.g. "1-0-1" (morning-afternoon-night) */
    @Column(nullable = false, length = 50)
    private String frequency;

    /** e.g. "5 days" */
    @Column(nullable = false, length = 50)
    private String duration;

    @Column(length = 255)
    private String instructions;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @PrePersist
    void applyDefaults() {
        if (sortOrder == null) sortOrder = 0;
    }
}
