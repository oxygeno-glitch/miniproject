package com.project.mini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupRequestDto {
    private String name;
    private String account;
    private String password;
}