package com.nyxeira.homerunner.entries.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarController {

    @GetMapping("/")
    public String list() {
        return "calendarview/index";
    }
}
