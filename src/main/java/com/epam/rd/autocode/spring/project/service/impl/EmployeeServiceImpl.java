package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(employee -> modelMapper.map(employee, EmployeeDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDTO getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Працівника з email " + email + " не знайдено"));
        return modelMapper.map(employee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public EmployeeDTO addEmployee(EmployeeDTO employeeDTO) {
        Employee employee = modelMapper.map(employeeDTO, Employee.class);
        Employee savedEmployee = employeeRepository.save(employee);
        return modelMapper.map(savedEmployee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public EmployeeDTO updateEmployeeByEmail(String email, EmployeeDTO employeeDTO) {
        Employee existingEmployee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Працівника не знайдено"));

        existingEmployee.setName(employeeDTO.getName());
        existingEmployee.setPassword(employeeDTO.getPassword());
        existingEmployee.setBirthDate(employeeDTO.getBirthDate());
        existingEmployee.setPhone(employeeDTO.getPhone());

        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        return modelMapper.map(updatedEmployee, EmployeeDTO.class);
    }

    @Override
    @Transactional
    public void deleteEmployeeByEmail(String email) {
        employeeRepository.deleteByEmail(email);
    }
}