package com.oms.order.service;

import com.oms.order.dto.AuthRequest;
import com.oms.order.dto.AuthResponse;
import com.oms.order.dto.RegisterRequest;
import com.oms.order.entity.User;
import com.oms.order.repository.UserRepository;
import com.oms.order.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = (User) auth.getPrincipal();
        String token = jwtUtil.generateToken(user);
        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }
}
