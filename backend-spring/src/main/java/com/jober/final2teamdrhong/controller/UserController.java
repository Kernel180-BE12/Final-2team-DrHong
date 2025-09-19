package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
@Tag(name = "사용자 관리", description = "사용자 정보 관리 API")
public class UserController {

    private final UserService userService;

    // TODO: 추후 사용자 CRUD API 구현
}