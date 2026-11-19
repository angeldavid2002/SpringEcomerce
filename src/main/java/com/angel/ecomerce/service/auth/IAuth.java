package com.angel.ecomerce.service.auth;

import com.angel.ecomerce.dto.auth.LoginRequest;
import com.angel.ecomerce.dto.auth.TokenResponse;

public interface IAuth {
    TokenResponse login(LoginRequest request);
}
