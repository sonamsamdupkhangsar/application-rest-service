package me.sonam.application.handler;

import java.util.UUID;

public class ApplicationBody {
    private UUID id;
    private String name;
    private String clientId;
    private UUID creatorUserId;
    private UUID organizationId;
    private String userRole;
    private String groupNames;

    public ApplicationBody(UUID id, String name, String clientId, UUID creatorUserId, UUID organizationId, String userRole, String groupNames) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.creatorUserId = creatorUserId;
        this.organizationId = organizationId;
        this.userRole = userRole;
        this.groupNames = groupNames;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId;
    }

    public UUID getCreatorUserId() {
        return this.creatorUserId;
    }

    public UUID getOrganizationId() {
        return this.organizationId;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getGroupNames() {
        return groupNames;
    }

    @Override
    public String toString() {
        return "ApplicationBody{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", clientId='" + clientId + '\'' +
                ", creatorUserId=" + creatorUserId +
                ", organizationId=" + organizationId +
                ", userRole='" + userRole + '\'' +
                ", groupNames='" + groupNames + '\'' +
                '}';
    }
}
