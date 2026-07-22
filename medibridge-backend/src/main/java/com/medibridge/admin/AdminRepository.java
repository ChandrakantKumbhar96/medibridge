package com.medibridge.admin;

import com.medibridge.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    Optional<Admin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
