package me.sonam.application.handler;

import java.util.UUID;

public class UserUpdate {
    public enum UpdateAction {
        add, update, delete
    }
    private UUID userId;
    private UpdateAction update;
    private String role;

    public UserUpdate(UUID userId, String role, String update) {
        this.update = UpdateAction.valueOf(update);
        this.role = role;
        this.userId = userId;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public UpdateAction getUpdate() {
        return this.update;
    }
    public String getRole() { return this.role; }
}
