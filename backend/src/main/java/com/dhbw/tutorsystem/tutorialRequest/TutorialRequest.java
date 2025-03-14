package com.dhbw.tutorsystem.tutorialRequest;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.dhbw.tutorsystem.specialisationCourse.SpecialisationCourse;
import com.dhbw.tutorsystem.user.student.Student;

import lombok.Getter;
import lombok.Setter;

@Entity
public class TutorialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Getter
    @Setter
    private SpecialisationCourse specialisationCourse;

    @OneToMany(fetch = FetchType.LAZY)
    @Getter
    @Setter
    private Set<Student> interestedStudents;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    private Student createdBy;

    @Getter
    @Setter
    private Integer semester;
}
