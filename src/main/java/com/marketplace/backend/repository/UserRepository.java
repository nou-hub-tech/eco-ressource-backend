package com.marketplace.backend.repository;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;

import java.util.List;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  @Query(
      "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.enterprise LEFT JOIN FETCH u.transporter"
          + " WHERE u.email = :email")
  Optional<User> findByEmailWithProfiles(@Param("email") String email);

  boolean existsByEmail(String email);

  Optional<User> findByEmailAndRole(String email, Role role);

  @Query(
      "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.enterprise LEFT JOIN FETCH u.transporter")
  java.util.List<User> findAllWithProfiles();

  @Query(
      "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.enterprise LEFT JOIN FETCH u.transporter"
          + " WHERE u.id = :id")
  Optional<User> findByIdWithProfiles(@Param("id") Long id);


  /** Récupère tous les utilisateurs d'un rôle donné (avec profils chargés en eager) */
  @Query(
      "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.enterprise LEFT JOIN FETCH u.transporter"
          + " WHERE u.role = :role")
  List<User> findByRoleWithProfiles(@Param("role") Role role);


}
