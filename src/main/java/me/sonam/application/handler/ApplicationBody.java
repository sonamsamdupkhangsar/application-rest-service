package me.sonam.application.handler;

import java.util.UUID;

public class ApplicationBody {
    private UUID id;
    private String name;
    private String clientId;

    public ApplicationBody(UUID id, String name, String clientId) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
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

}
