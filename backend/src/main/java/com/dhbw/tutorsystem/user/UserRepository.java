package com.dhbw.tutorsystem.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByFirstNameAndLastName(String firstname, String lastname);

    boolean existsByIdIn(Set<Integer> ids);

    @Query("SELECT u from  User u inner join u.roles ur where not ur.name = 'ROLE_ADMIN' ")
    List<User> findAllUsersThatAreNotAdmin();

    @Query("SELECT u FROM User u INNER JOIN u.roles ur WHERE ur.name = 'ROLE_STUDENT'")
    List<User> findAllStudents();

    @Query("SELECT u FROM User u INNER JOIN u.roles ur WHERE ur.name = 'ROLE_DIRECTOR'")
    List<User> findAllDirectors();

}
