package me.sonam.application.handler;

import java.util.UUID;

public class ApplicationBody {
    private UUID id;
    private String name;
    private String clientId;
    private UUID creatorUserId;
    private UUID organizationId;

    public ApplicationBody(UUID id, String name, String clientId, UUID creatorUserId, UUID organizationId) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.creatorUserId = creatorUserId;
        this.organizationId = organizationId;
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
}
