package com.angel.ecomerce.service.auth;

import com.angel.ecomerce.dto.auth.LoginRequest;
import com.angel.ecomerce.dto.auth.LogoutRequest;
import com.angel.ecomerce.dto.auth.RefreshRequest;
import com.angel.ecomerce.dto.auth.RefreshResponse;
import com.angel.ecomerce.dto.auth.RegisterRequest;
import com.angel.ecomerce.dto.auth.TokenResponse;

public interface IAuth {
    TokenResponse login(LoginRequest request);

    TokenResponse register(RegisterRequest request);

    void logout(LogoutRequest request);

    RefreshResponse refresh(RefreshRequest request);
}
