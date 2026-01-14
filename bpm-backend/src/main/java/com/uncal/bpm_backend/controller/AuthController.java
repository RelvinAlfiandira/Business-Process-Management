package com.uncal.bpm_backend.controller;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uncal.bpm_backend.dto.LoginRequest;
import com.uncal.bpm_backend.dto.RegisterRequest;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.repository.UserRepository;
import com.uncal.bpm_backend.security.JwtService;
import com.uncal.bpm_backend.service.EmailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;


    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Cek jika username sudah ada
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return new ResponseEntity<>("Username sudah digunakan!", HttpStatus.BAD_REQUEST);
        }
        // Cek jika email sudah ada
        if (registerRequest.getEmail() != null && userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>("Email sudah digunakan!", HttpStatus.BAD_REQUEST);
        }

        // Buat user baru dan simpan ke database
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        // Enkode password sebelum disimpan!
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);

        // Kirim email selamat datang dengan username dan password
        emailService.sendWelcomeEmail(registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword());

        return new ResponseEntity<>("User berhasil terdaftar!", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Lakukan otentikasi menggunakan Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            // Jika otentikasi gagal (username/password salah)
            return new ResponseEntity<>("Username atau password salah!", HttpStatus.UNAUTHORIZED);
        }

        // Jika berhasil, ambil UserDetails dan buat JWT
        User user = userRepository.findByUsername(loginRequest.getUsername()).get();
        // Karena kelas User kita belum mengimplementasikan UserDetails, kita anggap UserDetails-nya adalah user itu sendiri
        String jwt = jwtService.generateToken(user);

        // Kembalikan token ke frontend
        return ResponseEntity.ok(Collections.singletonMap("token", jwt));
    }

    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken() {
        return ResponseEntity.ok(Collections.singletonMap("message", "token valid"));
    }
}
