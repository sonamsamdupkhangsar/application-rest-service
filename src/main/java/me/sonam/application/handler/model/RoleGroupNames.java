package me.sonam.application.handler.model;

public class RoleGroupNames {
    private String userRole;
    private String[] groupNames;

    public RoleGroupNames(String userRole, String groupNames) {
        this.userRole = userRole;
        if (groupNames != null) {
            this.groupNames = groupNames.split(",");
        }
    }

    public String getUserRole() {
        return this.userRole;
    }

    public String[] getGroupNames() {
        return groupNames;
    }
}
