package com.absolutecinema.controller;

import com.absolutecinema.entity.Employee;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import com.absolutecinema.service.EmployeeService;
import com.absolutecinema.service.ShiftService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ShiftService shiftService;
    private final UserRepository userRepository;

    public EmployeeController(EmployeeService employeeService, ShiftService shiftService,
            UserRepository userRepository) {
        this.employeeService = employeeService;
        this.shiftService = shiftService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName());
        Employee employee = employeeService.findByEmail(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No employee record for " + user.getUsername()));

        model.addAttribute("firstname", employee.getFirstname());
        model.addAttribute("month", YearMonth.now().atDay(1));
        model.addAttribute("salary", employee.getSalary());
        model.addAttribute("shifts", shiftService.getCurrentMonthShifts(employee));
        return "employee/dashboard";
    }
}
