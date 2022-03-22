package com.dhbw.tutorsystem.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.List;
import java.util.Set;

import com.dhbw.tutorsystem.role.ERole;
import com.dhbw.tutorsystem.user.User;
import com.dhbw.tutorsystem.user.UserController;
import com.dhbw.tutorsystem.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void notLoggedIn() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "adam.admin@dhbw-mannheim.de", password = "1234", roles = "ADMIN")
    void loggedInAsAdmin() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "s111111@student.dhbw-mannheim.de", password = "1234", roles = "STUDENT")
    void loggedInAsStudent() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dirk.director@dhbw-mannheim.de", password = "1234", roles = "DIRECTOR")
    void loggedInAsDirector() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isOk());
    }

    @Test
    @Transactional
    @WithMockUser(username = "s111111@student.dhbw-mannheim.de", password = "1234", roles = "STUDENT")
    void checkIfAdminsAreNotIncluded() throws Exception {

        List<User> userList = userRepository.findAllUsersThatAreNotAdmin();
        // seven Users in Database, one of them is admin --> 6 users are the expected
        // output
        assertEquals(6, userList.size());

        for (User user : userList) {
            // test if current user has role ROLE_STUDENT or ROLE_DIRECTOR
            assertTrue(user.getRoles().iterator().next().getName().equals(ERole.ROLE_STUDENT)
                    || user.getRoles().iterator().next().getName().equals(ERole.ROLE_DIRECTOR));
        }
    }

}
