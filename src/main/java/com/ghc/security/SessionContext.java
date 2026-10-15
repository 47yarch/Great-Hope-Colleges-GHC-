package com.ghc.security;

public final class SessionContext {
    private static UserSession currentUser;

    private SessionContext() {}

    public static synchronized void signIn(UserSession user) {
        if (user == null) {
            throw new IllegalArgumentException("User session is required.");
        }
        currentUser = user;
    }

    public static synchronized UserSession currentUser() {
        if (currentUser == null) {
            throw new SecurityException(
                    "You must sign in before accessing this operation.");
        }
        return currentUser;
    }

    public static synchronized boolean isSignedIn() {
        return currentUser != null;
    }

    public static synchronized void signOut() {
        currentUser = null;
    }
}
