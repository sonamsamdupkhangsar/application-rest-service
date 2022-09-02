package me.sonam.application.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

public class ApplicationUser implements Persistable<UUID> {
    public enum RoleNamesEnum {
        admin, user
    }
    @Id
    private UUID id;
    private UUID applicationId;
    private UUID userId;
    private String roleName;

    public ApplicationUser(UUID id, UUID applicationId, UUID userId, String roleName) {
        if (id == null) {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        else {
            this.id = id;
            this.isNew = false;
        }
        this.applicationId = applicationId;
        this.userId = userId;
        this.roleName = roleName;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRoleName() {
        return roleName;
    }

    @Transient
    private boolean isNew;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}