package com.backend.naildp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.naildp.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
