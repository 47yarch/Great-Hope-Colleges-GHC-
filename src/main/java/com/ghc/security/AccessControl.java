package com.ghc.security;

public final class AccessControl {
    private AccessControl() {}

    public static UserSession requireSignedIn() {
        return SessionContext.currentUser();
    }

    public static UserSession requireAdmin() {
        UserSession user = requireSignedIn();
        if (!user.isAdmin()) {
            throw new SecurityException(
                    "Only an Administrator can perform this operation.");
        }
        return user;
    }

    public static UserSession requireAdminOrRegistrar() {
        UserSession user = requireSignedIn();
        if (!user.isAdmin() && !user.isRegistrar()) {
            throw new SecurityException(
                    "Only an Administrator or Registrar can perform this operation.");
        }
        return user;
    }

    public static UserSession requireGradeAccess() {
        UserSession user = requireSignedIn();
        if (!user.isAdmin() && !user.isInstructor()) {
            throw new SecurityException(
                    "You do not have permission to manage grades.");
        }
        return user;
    }
}