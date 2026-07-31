package com.feple.feple_backend.auth.jwt;

public final class JwtConstants {
    private JwtConstants() {}

    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_LENGTH = BEARER_PREFIX.length();

    public static final String CLAIM_TYPE = "type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
}
