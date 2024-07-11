package com.backend.naildp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.naildp.entity.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
}
