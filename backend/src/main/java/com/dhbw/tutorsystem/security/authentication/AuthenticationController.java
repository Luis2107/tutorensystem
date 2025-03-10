package com.dhbw.tutorsystem.security.authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.validation.Valid;

import com.dhbw.tutorsystem.exception.TSExceptionResponse;
import com.dhbw.tutorsystem.exception.TSInternalServerException;
import com.dhbw.tutorsystem.mails.EmailSenderService;
import com.dhbw.tutorsystem.mails.MailType;
import com.dhbw.tutorsystem.role.ERole;
import com.dhbw.tutorsystem.role.Role;
import com.dhbw.tutorsystem.role.RoleRepository;
import com.dhbw.tutorsystem.security.authentication.exception.AccountNotEnabledException;
import com.dhbw.tutorsystem.security.authentication.exception.EmailAlreadyExistsException;
import com.dhbw.tutorsystem.security.authentication.exception.InvalidUserTypeException;
import com.dhbw.tutorsystem.security.authentication.exception.LastPasswordActionTooRecentException;
import com.dhbw.tutorsystem.security.authentication.exception.LoginFailedException;
import com.dhbw.tutorsystem.security.authentication.exception.RoleNotFoundException;
import com.dhbw.tutorsystem.security.authentication.exception.UserAlreadyEnabledException;
import com.dhbw.tutorsystem.security.authentication.exception.UserNotFoundException;
import com.dhbw.tutorsystem.security.authentication.payload.ChangePasswordRequest;
import com.dhbw.tutorsystem.security.authentication.payload.JwtResponse;
import com.dhbw.tutorsystem.security.authentication.payload.LoginRequest;
import com.dhbw.tutorsystem.security.authentication.payload.RegisterRequest;
import com.dhbw.tutorsystem.security.authentication.payload.RequestPasswordResetRequest;
import com.dhbw.tutorsystem.security.authentication.payload.ResetPasswordRequest;
import com.dhbw.tutorsystem.security.authentication.payload.VerifyRequest;
import com.dhbw.tutorsystem.security.jwt.JwtUtils;
import com.dhbw.tutorsystem.security.services.UserDetailsImpl;
import com.dhbw.tutorsystem.specialisationCourse.SpecialisationCourse;
import com.dhbw.tutorsystem.specialisationCourse.SpecialisationCourseRepository;
import com.dhbw.tutorsystem.tutorial.exception.SpecialisationCourseNotFoundException;
import com.dhbw.tutorsystem.user.User;
import com.dhbw.tutorsystem.user.UserRepository;
import com.dhbw.tutorsystem.user.UserService;
import com.dhbw.tutorsystem.user.director.Director;
import com.dhbw.tutorsystem.user.director.DirectorRepository;
import com.dhbw.tutorsystem.user.student.Student;
import com.dhbw.tutorsystem.user.student.StudentRepository;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/authentication")
@SecurityScheme(name = "jwt-auth", type = SecuritySchemeType.HTTP, scheme = "bearer")
@RequiredArgsConstructor
public class AuthenticationController {

    @Value("${backend.app.minutesBetweenPasswordActions}")
    private int minimumMinutesBetweenPasswordActions;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder encoder;
    private final EmailSenderService emailSenderService;
    private final UserService userService;
    private final StudentRepository studentRepository;
    private final DirectorRepository directorRepository;
    private final SpecialisationCourseRepository specialisationCourseRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    // request of a user to login (by email and password)
    // if logged in successfully, a jwt token with separate roles, expirationDate
    // and email is returned
    @Operation(summary = "Login a user based on email and password.", tags = { "authentication" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login was successful. User is logged by using the token in the response.", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "Login was not successful.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // try to authenticate a user by email and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            // if succesful, get the detailsImpl of the user for better handling
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            // generate the jwt Token
            String jwt = jwtUtils.generateJwtTokenFromUsername(userDetails.getEmailAddress());
            // get the roles of the user
            List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // reset temp password so that any previous "forgot password" action is
            // invalidated
            User user = userRepository.findById(userDetails.getUserId()).get();
            if (user.getTempPassword() != null) {
                user.setTempPassword(null);
                userRepository.save(user);
            }

            return ResponseEntity.ok(new JwtResponse(roles, jwt, jwtUtils.getExpirationDateFromJwtToken(jwt),
                    userDetails.getEmailAddress()));
        } catch (AuthenticationException e) {
            logger.error("Unauthorized error: {}", e.getMessage());
            throw new LoginFailedException();
        }
    }

    @Operation(summary = "Create user.", description = "Create a not-enabled account for a user using email and password and send registration email with activation link.", tags = {
            "authentication" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration email with activation link was sent successfully."),
            @ApiResponse(responseCode = "400", description = "Error in processing defined by error message and error code in response.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // get user (either student or director) to check for duplicate registration
        User user = null;

        // check type of email and get user if it exists from db
        // a user can exist, if he registerd earlier but did not manage to accept the
        // invitation in time
        if (User.isStudentMail(registerRequest.getEmail())) {
            Optional<Student> optionalStudent = studentRepository.findByEmail(registerRequest.getEmail());
            if (optionalStudent.isPresent())
                user = (User) optionalStudent.get();
        } else if (User.isDirectorMail(registerRequest.getEmail())) {
            Optional<Director> optionalDirector = directorRepository.findByEmail(registerRequest.getEmail());
            if (optionalDirector.isPresent())
                user = (User) optionalDirector.get();
        }

        // check if a user was registered earlier or not by checking the user variable
        if (user != null) {
            // existing user is registering again

            // an enabled user could try to be registered again with the same email -> throw
            // EmailAlreadyExistsException
            if (user.isEnabled()) {
                logger.info("User with email {} already exists", user.getEmail());
                throw new EmailAlreadyExistsException();
            }

            // check if enough time is between the last and the current registration. If the
            // time between the last two registrations is smaller than
            // minimumMinutesBetweenPasswordActions throw exception
            if (user.getLastPasswordAction() != null
                    && Duration.between(user.getLastPasswordAction(), LocalDateTime.now())
                            .toMinutes() < minimumMinutesBetweenPasswordActions) {
                logger.info("Registration link wasn't used in time: {}",
                        LastPasswordActionTooRecentException.class.getSimpleName());
                throw new LastPasswordActionTooRecentException();
            } else {
                // existing non-enabled user re-registered after 15minutes: re-send email and
                // update last changed
                user.setLastPasswordAction(LocalDateTime.now());
                user.setPassword(encoder.encode(registerRequest.getPassword()));
                user.setFirstName(registerRequest.getFirstName());
                user.setLastName(registerRequest.getLastName());

                // resend mail after update
                try {
                    sendRegisterMail(user.getEmail(), user.getLastPasswordAction(), false);
                } catch (NoSuchAlgorithmException | MessagingException e) {
                    logger.error("Unauthorized error: {}", e.getMessage());
                    throw new TSInternalServerException();
                }

                // save user to db
                saveUserSubtype(user);
            }
        } else {
            // new user registration: encode password and save new user
            String encodedPassword = encoder.encode(registerRequest.getPassword());

            // create new user
            user = new User(
                    registerRequest.getFirstName(), registerRequest.getLastName(),
                    registerRequest.getEmail(), encodedPassword);
            user.setEnabled(false);
            user.setLastPasswordAction(LocalDateTime.now());

            // get roles by email
            Optional<Role> role = Optional.empty();
            if (User.isStudentMail(user.getEmail())) {
                Student student = new Student(user.getEmail(), user.getPassword());
                Optional<SpecialisationCourse> specialisationCourse = specialisationCourseRepository
                        .findById(registerRequest.getSpecialisationCourseId());
                if (specialisationCourse.isEmpty()) {
                    throw new SpecialisationCourseNotFoundException();
                }
                student.setSpecialisationCourse(specialisationCourse.get());
                user = student;
                role = roleRepository.findByName(ERole.ROLE_STUDENT);
            } else if (User.isDirectorMail(user.getEmail())) {
                user = new Director(user.getEmail(), user.getPassword());
                role = roleRepository.findByName(ERole.ROLE_DIRECTOR);
            }

            // role might not be available in DB
            if (role.isEmpty()) {
                logger.error("User ({}) found with no valid role", user.getEmail());
                throw new RoleNotFoundException();
            } else {
                // set roles for the created user
                user.setRoles(Set.of(role.get()));
            }

            // send registration mail
            try {
                sendRegisterMail(user.getEmail(), user.getLastPasswordAction(), true);
            } catch (NoSuchAlgorithmException | MessagingException e) {
                logger.error("Unauthorized error: {}", e.getMessage());
                throw new TSInternalServerException();
            }

            // save user based on Subtype for choosing the correct repo
            saveUserSubtype(user);
        }
        return ResponseEntity.ok(null);
    }

    // saves a user in the correct repo
    private User saveUserSubtype(User user) throws InvalidUserTypeException {
        // check the type of the provided user
        if (user instanceof Student) {
            return studentRepository.save((Student) user);
        } else if (user instanceof Director) {
            return directorRepository.save((Director) user);
        } else {
            throw new InvalidUserTypeException();
        }
    }

    // enable an account by clicking on the link in the email
    @Operation(summary = "Enable user.", description = "Enable an account using a hash value from an activation link after registration.", tags = {
            "authentication" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account was activated successfully. User will be directly logged in using token in response."),
            @ApiResponse(responseCode = "400", description = "Error in processing defined by error message and error code in response.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PostMapping("/enableAccount")
    public ResponseEntity<?> enableUserAccount(@Valid @RequestBody VerifyRequest verifyRequest) {
        // find user by email and enable if hash is valid
        Optional<User> optionalUser = userRepository.findByEmail(verifyRequest.getEmail());
        if (optionalUser.isEmpty()) {
            logger.error("Email {} from verifying account was not found", verifyRequest.getEmail());
            throw new UserNotFoundException();
        }
        // check if user is enabled and throw exception
        if (optionalUser.get().isEnabled()) {
            logger.info("Verifying link for {} was already used", verifyRequest.getEmail());
            throw new UserAlreadyEnabledException();
        }
        // get user for better interaction
        User user = optionalUser.get();
        // recreate hash and check if it is vaild
        if (isHashClaimValid(verifyRequest.getHash(), user.getEmail(), user.getLastPasswordAction().toString())) {
            // hash is valid: activate user and return jwt
            user.setEnabled(true);
            user.setLastPasswordAction(LocalDateTime.now());
            user = userRepository.save(user);
            String jwt = jwtUtils.generateJwtTokenFromUsername(user.getEmail());
            return ResponseEntity.ok(new JwtResponse(Role.getRolesString(user.getRoles()), jwt,
                    jwtUtils.getExpirationDateFromJwtToken(jwt),
                    user.getEmail()));
        } else {
            // hash is not valid: return exception
            logger.error("Internal Server Exception: {}", TSInternalServerException.class.getSimpleName());
            throw new TSInternalServerException();
        }
    }

    // Route for requesting a reset of the password. As a result, an email with a
    // reset link is sent.
    @Operation(summary = "Request a password reset for forgotten password.", description = "Request to reset the password corresponding to a user account by email. Sends an email containing a link to reset the password.", tags = {
            "authentication" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email was sent successfully."),
            @ApiResponse(responseCode = "400", description = "Error in processing defined by error message and error code in response.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PostMapping("/requestPasswordReset")
    public ResponseEntity<?> resetPasswordRequest(
            @Valid @RequestBody RequestPasswordResetRequest requestPasswordResetRequest) {
        // find user by email
        Optional<User> optionalUser = userRepository.findByEmail(requestPasswordResetRequest.getEmail());
        if (optionalUser.isEmpty()) {
            logger.info("User with email {} was not found", requestPasswordResetRequest.getEmail());
            throw new UserNotFoundException();
        }
        User user = optionalUser.get();
        // if the user isn't enabled he can not reset the password because he must
        // re-register then.
        if (!user.isEnabled()) {
            logger.info("User with email {} has not been enabled yet", requestPasswordResetRequest.getEmail());
            throw new AccountNotEnabledException();
        }

        // check if time between last password reset and now is to small
        if (Duration.between(user.getLastPasswordAction(), LocalDateTime.now())
                .toMinutes() < minimumMinutesBetweenPasswordActions) {
            logger.info("User with email {} took too long to reset password", requestPasswordResetRequest.getEmail());
            throw new LastPasswordActionTooRecentException();
        }
        try {
            // refresh last password action for user and send mail
            LocalDateTime newLastPasswordAction = LocalDateTime.now();
            sendResetPasswordMail(user.getEmail(), newLastPasswordAction,
                    requestPasswordResetRequest.getNewPassword());
            user.setLastPasswordAction(newLastPasswordAction);

            // just set the temp password in case person b resets the password for person a,
            // person a can still login with the original password
            user.setTempPassword(encoder.encode(requestPasswordResetRequest.getNewPassword()));
            userRepository.save(user);
            return ResponseEntity.ok(null);
        } catch (NoSuchAlgorithmException | MessagingException e) {
            logger.error("Unauthorized error: {}", e.getMessage());
            throw new TSInternalServerException();
        }
    }

    // perform password reset by clicking the link in the mail sent by request
    // password reset
    @Operation(summary = "Reset a password that was forgotten.", description = "Reset the password using a hash value and the new password from a link received after a password reset request.", tags = {
            "authentication" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password was reset successfully. User will be logged in directly using token in response."),
            @ApiResponse(responseCode = "400", description = "Error in processing defined by error message and error code in response.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PostMapping("/performPasswordReset")
    public ResponseEntity<JwtResponse> resetUserPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(resetPasswordRequest.getEmail());
        // not-enabled users should follow the register email link first, so throw
        // exception
        if (optionalUser.isEmpty()) {
            logger.error("Internal Server Exception: {}", TSInternalServerException.class.getSimpleName());
            throw new TSInternalServerException();
        }
        User user = optionalUser.get();
        // Check if user was enabled and a temp password was set. Both should be the
        // case because it is already checked in resetPasswordRequest, so this is a
        // fallback
        if (!user.isEnabled() || user.getTempPassword() == null) {
            logger.error("Internal Server Exception: {}", TSInternalServerException.class.getSimpleName());
            throw new TSInternalServerException();
        }

        // check hash of the email and the provided data: if they are the same, reset
        // password, else throw exception
        if (isHashClaimValid(resetPasswordRequest.getHash(), user.getEmail(),
                user.getLastPasswordAction().toString(), resetPasswordRequest.getNewPassword())) {
            // set new encoded password, update last action, delete temp password and
            // directly login user
            user.setPassword(encoder.encode(resetPasswordRequest.getNewPassword()));
            user.setLastPasswordAction(LocalDateTime.now());
            user.setTempPassword(null);
            user = userRepository.save(user);
            String jwt = jwtUtils.generateJwtTokenFromUsername(user.getEmail());

            // jwt token is sent to frontend to login user
            return ResponseEntity.ok(new JwtResponse(Role.getRolesString(user.getRoles()), jwt,
                    jwtUtils.getExpirationDateFromJwtToken(jwt),
                    user.getEmail()));
        } else {
            logger.error("Internal Server Exception: {}", TSInternalServerException.class.getSimpleName());
            throw new TSInternalServerException();
        }
    }

    // logged-in user wants to change his password
    @Operation(summary = "Change a password while being logged in.", description = "Change the password into a new password when being logged in. Only Students and Directors can reset their passwords.", tags = {
            "authentication" }, security = @SecurityRequirement(name = "jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password was changed successfully. The user will be directly logged in using the token in the response."),
            @ApiResponse(responseCode = "400", description = "Error in processing defined by error message and error code in response.", content = @Content(schema = @Schema(implementation = TSExceptionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_DIRECTOR')")
    @PostMapping("/changePassword")
    public ResponseEntity<JwtResponse> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        // find user and change the password, then directly log in with new token
        User loggedUser = userService.getLoggedInUser();
        if (loggedUser == null) {
            logger.info("Logged in user was not found");
            throw new UserNotFoundException();
        }
        Optional<User> optionalUser = userRepository.findByEmail(loggedUser.getEmail());
        // this should not occur, because user is logged in so this serves as a fallback
        if (optionalUser.isEmpty()) {
            logger.info("User with email {} was not found", loggedUser.getEmail());
            throw new UserNotFoundException();
        }
        User user = optionalUser.get();
        // this should not occur, because user is logged in so this serves as a fallback
        if (!user.isEnabled()) {
            logger.info("User with email {} has not been enabled yet", user.getEmail());
            throw new AccountNotEnabledException();
        }
        // user can only reset the password every minimumMinutesBetweenPasswordActions
        // minutes
        if (Duration.between(user.getLastPasswordAction(),
                LocalDateTime.now()).toMinutes() < minimumMinutesBetweenPasswordActions) {
            logger.info("User with email {} took too long to reset password", user.getEmail());
            throw new LastPasswordActionTooRecentException();
        }
        // set new password and update user
        user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
        user.setLastPasswordAction(LocalDateTime.now());
        user = userRepository.save(user);

        // return jwt to login user
        String jwt = jwtUtils.generateJwtTokenFromUsername(user.getEmail());
        return ResponseEntity.ok(new JwtResponse(Role.getRolesString(user.getRoles()), jwt,
                jwtUtils.getExpirationDateFromJwtToken(jwt),
                user.getEmail()));
    }

    private void sendRegisterMail(String userMail, LocalDateTime lastPasswordAction, boolean isFirstRegisterMail)
            throws NoSuchAlgorithmException, MessagingException {
        try {
            // create hash of mail and lastpasswordAction
            String hashBase64 = createBase64VerificationHash(userMail, lastPasswordAction.toString());
            // send mail
            emailSenderService.sendMail(userMail, MailType.REGISTRATION, Map.of(
                    "hashBase64", hashBase64,
                    "isFirstRegisterMail", isFirstRegisterMail));
        } catch (NoSuchAlgorithmException | MessagingException e) {
            logger.error("Unauthorized error: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void sendResetPasswordMail(String userMail, LocalDateTime lastPasswordAction, String newPassword)
            throws NoSuchAlgorithmException, MessagingException {
        try {
            // create hash of mail and lastpasswordAction and password
            String hashBase64 = createBase64VerificationHash(userMail, lastPasswordAction.toString(), newPassword);
            // send mail
            emailSenderService.sendMail(userMail, MailType.RESET_PASSWORD, Map.of(
                    "hashBase64", hashBase64));
        } catch (NoSuchAlgorithmException | MessagingException e) {
            logger.error("Unauthorized error: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean isHashClaimValid(String hashClaim, String... rawComponents) {
        try {
            // create a hash of all provided string components
            String hashBase64Expected = createBase64VerificationHash(rawComponents);
            // check if hashes are equal
            return StringUtils.equals(hashBase64Expected, hashClaim);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unauthorized error: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // create a hash of all provided string components
    private String createBase64VerificationHash(String... rawComponents) throws NoSuchAlgorithmException {
        String rawText = Stream.of(rawComponents).reduce("", (s1, s2) -> s1 + s2);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                rawText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}
