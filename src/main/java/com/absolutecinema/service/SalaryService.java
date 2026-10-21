package com.absolutecinema.service;

import com.absolutecinema.entity.Employee;
import com.absolutecinema.entity.Salary;
import com.absolutecinema.entity.Shift;
import com.absolutecinema.repository.SalaryRepository;
import com.absolutecinema.repository.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeService employeeService;

    @Autowired
    public SalaryService(
            SalaryRepository salaryRepository,
            ShiftRepository shiftRepository,
            EmployeeService employeeService) {
        this.salaryRepository = salaryRepository;
        this.shiftRepository = shiftRepository;
        this.employeeService = employeeService;
    }

    public Salary getCurrentMonthSalary(Long employeeId) {
        Employee employee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        YearMonth currentMonth = YearMonth.now();
        return salaryRepository.findByEmployeeAndYearMonth(employee, currentMonth)
                .orElseGet(() -> calculateSalaryForMonth(employee, currentMonth));
    }

    public List<Shift> getCurrentMonthShifts(Long employeeId) {
        Employee employee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        return shiftRepository.findByEmployeeAndStartTimeBetweenOrderByStartTimeDesc(
                employee, startOfMonth, endOfMonth);
    }

    private Salary calculateSalaryForMonth(Employee employee, YearMonth yearMonth) {
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Shift> shifts = shiftRepository.findByEmployeeAndStartTimeBetweenOrderByStartTimeDesc(
                employee, startOfMonth, endOfMonth);

        BigDecimal totalHours = shifts.stream()
                .map(Shift::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assuming a default hourly rate of $15
        BigDecimal hourlyRate = new BigDecimal("15.00");
        BigDecimal totalSalary = hourlyRate.multiply(totalHours);

        Salary salary = new Salary();
        salary.setEmployee(employee);
        salary.setYearMonth(yearMonth);
        salary.setHourlyRate(hourlyRate);
        salary.setTotalHours(totalHours);
       // salary.setTotalSalary(totalSalary);

        return salaryRepository.save(salary);
    }

    public Shift recordShift(Long employeeId, LocalDateTime startTime, LocalDateTime endTime) {
        Employee employee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Calculate hours worked
        BigDecimal hours = BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        Shift shift = new Shift();
        shift.setEmployee(employee);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setHours(hours);

        return shiftRepository.save(shift);
    }
}