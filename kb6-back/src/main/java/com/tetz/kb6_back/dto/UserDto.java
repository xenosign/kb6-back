package com.tetz.kb6_back.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // Jackson이 타입 정보를 저장하기 위한 필드
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private String id;
    private String name;
    private int age;
}