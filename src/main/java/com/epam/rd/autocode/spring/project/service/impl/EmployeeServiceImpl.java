package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(employee -> modelMapper.map(employee, EmployeeDTO.class));
    }

    @Override
    public EmployeeDTO getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Працівника з email " + email + " не знайдено"));
        return modelMapper.map(employee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public EmployeeDTO addEmployee(EmployeeDTO employeeDTO) {
        Employee employee = modelMapper.map(employeeDTO, Employee.class);

        employee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));

        Employee savedEmployee = employeeRepository.save(employee);
        return modelMapper.map(savedEmployee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public EmployeeDTO updateEmployeeByEmail(String email, EmployeeDTO employeeDTO) {
        Employee existingEmployee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Працівника не знайдено"));

        String currentPassword = existingEmployee.getPassword();

        modelMapper.map(employeeDTO, existingEmployee);

        if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
            existingEmployee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        } else {
            existingEmployee.setPassword(currentPassword);
        }

        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        return modelMapper.map(updatedEmployee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public void deleteEmployeeByEmail(String email) {
        employeeRepository.deleteByEmail(email);
    }

    @Override
    @Transactional
    public void updatePersonalData(String email, String phone, java.time.LocalDate birthDate) {
        Employee existingEmployee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Працівника не знайдено"));

        existingEmployee.setPhone(phone);
        existingEmployee.setBirthDate(birthDate);

        employeeRepository.save(existingEmployee);
    }
}