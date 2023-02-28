package me.sonam.application.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class Application implements Persistable<UUID> {

    @Id
    private UUID id;
    private String name;
    private String clientId;
    private LocalDateTime created;
    private UUID creatorUserId;

    private UUID organizationId;

    @Transient
    private boolean isNew;

    public Application(UUID id, String name, String clientId, UUID creatorUserId, UUID organizationId) {
        if (id == null) {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        else {
            this.id = id;
            this.isNew = false;
        }
        this.name = name;
        this.clientId = clientId;
        this.created = ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime();
        this.creatorUserId = creatorUserId;
        this.organizationId = organizationId;
    }


    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public UUID getCreatorUserId() {
        return this.creatorUserId;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getOrganizationId() {
        return this.organizationId;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", clientId='" + clientId + '\'' +
                ", created=" + created +
                ", creatorUserId=" + creatorUserId +
                ", isNew=" + isNew +
                ", organizationId="+organizationId +
                '}';
    }
}
