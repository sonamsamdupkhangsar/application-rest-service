package me.sonam.application.handler;

import java.util.List;
import java.util.UUID;

public class ApplicationUserBody {
    public enum UpdateAction {
        add, update, delete
    }

    private UUID id;
    private UUID applicationId;

    private UUID userId;
    private UpdateAction updateAction;
    private String userRole;
    private String groupNames;

    public ApplicationUserBody(UUID id, UUID applicationId, UUID userId, UpdateAction updateAction, String userRole, String groupNames) {
        this.id = id;
        this.applicationId = applicationId;
        this.userId = userId;
        this.updateAction = updateAction;
        this.userRole = userRole;
        this.groupNames = groupNames;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getApplicationId() {
        return this.applicationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UpdateAction getUpdateAction() {
        return updateAction;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getGroupNames() {
        return groupNames;
    }

    @Override
    public String toString() {
        return "ApplicationUserBody{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", userId=" + userId +
                ", updateAction=" + updateAction +
                ", userRole='" + userRole + '\'' +
                ", groupNames='" + groupNames + '\'' +
                '}';
    }
}
