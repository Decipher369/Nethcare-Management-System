package com.nethcare.repository;

import com.nethcare.model.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberSequence s where s.sequenceName = :name")
    Optional<NumberSequence> lockByName(@Param("name") String name);
}
