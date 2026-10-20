package com.absolutecinema.controller;

import com.absolutecinema.entity.Employee;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import com.absolutecinema.service.EmployeeService;
import com.absolutecinema.service.SalaryService;
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
    private final SalaryService salaryService;
    private final UserRepository userRepository;

    public EmployeeController(EmployeeService employeeService, SalaryService salaryService,
            UserRepository userRepository) {
        this.employeeService = employeeService;
        this.salaryService = salaryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName());
        // Staff records are linked to login accounts by email address
        Employee employee = employeeService.findByEmail(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No employee record for " + user.getUsername()));

        model.addAttribute("firstname", employee.getFirstname());
        model.addAttribute("month", YearMonth.now().atDay(1));
        model.addAttribute("salary", employee.getSalary());
        model.addAttribute("shifts", salaryService.getCurrentMonthShifts(employee.getId()));
        return "employee/dashboard";
    }
}
