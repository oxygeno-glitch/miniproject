package com.project.mini.controller;

import com.project.mini.dto.UserLoginRequestDto;
import com.project.mini.dto.UserSignupRequestDto;
import com.project.mini.entity.User;
import com.project.mini.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignupRequestDto request) {
        if (userRepository.findByAccount(request.getAccount()).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setAccount(request.getAccount());
        user.setPassword(request.getPassword()); // 실무에서는 암호화 필수, 테스트용으로 직관적 저장
        user.setName(request.getName());

        userRepository.save(user);
        return ResponseEntity.ok(true);
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDto request) {
        Optional<User> optionalUser = userRepository.findByAccount(request.getAccount());

        if (optionalUser.isEmpty() || !optionalUser.get().getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그인 성공");
        response.put("name", optionalUser.get().getName());

        return ResponseEntity.ok(response);
    }
}