package com.nethcare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleDefinition {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 20)
    private Role roleName;

    @Column(name = "description", nullable = false, length = 150)
    private String description;

    public Role getRoleName() { return roleName; }
    public void setRoleName(Role roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
