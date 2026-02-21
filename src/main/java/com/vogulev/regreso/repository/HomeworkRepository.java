package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    long countByClientIdAndStatus(UUID clientId, Homework.Status status);
}
