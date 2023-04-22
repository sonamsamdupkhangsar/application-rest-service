package me.sonam.application.handler.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleGroupNames {
    private static final Logger LOG = LoggerFactory.getLogger(RoleGroupNames.class);

    private String userRole;
    private String groupNames;

    public RoleGroupNames(String userRole, String groupNameCsv) {
        this.userRole = userRole;
        this.groupNames = groupNameCsv;
    }

    public String getUserRole() {
        return this.userRole;
    }
    public String getGroupNames() {
        return this.groupNames;
    }
}
