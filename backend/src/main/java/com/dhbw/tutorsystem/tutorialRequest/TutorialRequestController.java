package com.dhbw.tutorsystem.tutorialRequest;

import javax.validation.Valid;

import com.dhbw.tutorsystem.exception.TSExceptionResponse;
import com.dhbw.tutorsystem.security.authentication.exception.StudentNotLoggedInException;
import com.dhbw.tutorsystem.user.student.Student;
import com.dhbw.tutorsystem.user.student.StudentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/tutorialrequest")
@AllArgsConstructor
@SecurityScheme(name = "jwt-auth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class TutorialRequestController {

    private final TutorialRequestRepository tutorialRequestRepository;
    private final StudentService studentService;

    // create a Tutorial Offer by a student 
    @Operation(tags = {
            "tutorialRequest" }, summary = "Create new TutorialRequest.", description = "Creates a new TutorialRequest for the logged that must be a student.", security = @SecurityRequirement(name = "jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successful creation."),
            @ApiResponse(responseCode = "401", description = "No student was logged in.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class))),
    })
    @PutMapping
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<Void> createTutorialOffer(
            @RequestBody @Valid CreateTutorialRequestRequest createTutorialRequestRequest) {
        // find out which user executes this operation
        Student student = studentService.getLoggedInStudent();
        if (student != null) {
            TutorialRequest tutorialRequest = new TutorialRequest();
            tutorialRequest.setDescription(createTutorialRequestRequest.getDescription());
            tutorialRequest.setTitle(createTutorialRequestRequest.getTitle());
            tutorialRequest.setSemester(createTutorialRequestRequest.getSemester());
            tutorialRequest.setCreatedBy(student);
            tutorialRequest.setSpecialisationCourse(student.getSpecialisationCourse());
            tutorialRequestRepository.save(tutorialRequest);
            return new ResponseEntity<Void>(HttpStatus.CREATED);
        } else {
            throw new StudentNotLoggedInException();
        }
    }
}
