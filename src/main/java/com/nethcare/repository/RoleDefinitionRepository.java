package com.nethcare.repository;

import com.nethcare.model.Role;
import com.nethcare.model.RoleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDefinitionRepository extends JpaRepository<RoleDefinition, Role> { }
