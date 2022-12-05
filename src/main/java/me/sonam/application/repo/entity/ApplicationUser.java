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
    private String userRole;
    private String groupNames;

    public ApplicationUser(UUID id, UUID applicationId, UUID userId, String userRole, String groupNames) {
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
        this.userRole = userRole;
        this.groupNames = groupNames;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public java.lang.String getGroupNames() {
        return groupNames;
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
