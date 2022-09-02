package me.sonam.application.handler;

import java.util.List;
import java.util.UUID;

public class ApplicationUserBody {
    private UUID id;
    private UUID applicationId;
    private List<UserUpdate> userUpdateList;

    public ApplicationUserBody(UUID id, UUID applicationId, List<UserUpdate> userUpdateList) {
        this.id = id;
        this.applicationId = applicationId;
        this.userUpdateList = userUpdateList;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getApplicationId() {
        return this.applicationId;
    }

    public List<UserUpdate> getUserUpdateList() {
        return List.copyOf(userUpdateList);
    }
}
