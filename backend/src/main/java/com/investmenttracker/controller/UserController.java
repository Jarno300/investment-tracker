package com.investmenttracker.controller;

import com.investmenttracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refreshAccountData() {
    userService.refreshCurrentUserAccountData();
  }
}
