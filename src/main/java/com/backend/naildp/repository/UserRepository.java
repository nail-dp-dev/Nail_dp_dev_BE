package com.backend.naildp.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.naildp.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findUserByNickname(String nickname);
}
