package com.example.prj3be.dto;

import com.example.prj3be.constant.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberFormDto {
    @NotBlank(message = "아이디는 필수 입력값입니다.")
    private String logId;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    private String password;

    @Email(message = "이메일 형식에 맞게 입력해주세요.")
    private String email;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    private String name;

    @NotNull(message = "생년월일은 필수 입력값입니다.")
    private Integer birthDate;
    @NotNull(message = "필수 입력값 입니다.")
    private Integer firstDigit; // 주민등록번호 뒤 첫번째 숫자.


    @NotBlank(message = "주소는 필수 입력값입니다.")
    private String address;

    private Role role;
}
