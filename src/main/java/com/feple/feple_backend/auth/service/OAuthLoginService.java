package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.User;
import reactor.core.publisher.Mono;

public interface OAuthLoginService {
    Mono<User> authenticate(String credential);
}
