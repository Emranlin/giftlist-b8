package com.example.giftlistb8.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.List;

import static jakarta.persistence.CascadeType.*;

@Entity
@Table(name = "charities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Charity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charity_id_gen")
    @SequenceGenerator(name = "charity_id_gen",
            sequenceName = "charity_id_seq",allocationSize = 1,initialValue = 10)
    private Long id;
    private String name;
    private String state;
    private String category;
    private String subCategory;
    private String description;
    private LocalDate dateOfIssue;
    private String image;
    private Boolean status;
    private boolean isBlocked;

    @ManyToOne(cascade = {PERSIST, MERGE, REFRESH, DETACH})
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Complaint> complaints;

    @OneToOne(mappedBy = "charity", cascade = {PERSIST, MERGE, REFRESH, DETACH})
    private Reserve reserve;
}