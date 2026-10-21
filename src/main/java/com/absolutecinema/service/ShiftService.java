package com.absolutecinema.service;

import com.absolutecinema.entity.Employee;
import com.absolutecinema.entity.Shift;
import com.absolutecinema.repository.ShiftRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    /** The employee's shifts that start in the current calendar month, newest first. */
    public List<Shift> getCurrentMonthShifts(Employee employee) {
        YearMonth month = YearMonth.now();
        return shiftRepository.findByEmployeeAndStartTimeBetweenOrderByStartTimeDesc(
                employee, month.atDay(1).atStartOfDay(), month.atEndOfMonth().atTime(23, 59, 59));
    }
}
