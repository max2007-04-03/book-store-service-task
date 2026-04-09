package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageImpl;
 import org.springframework.data.domain.PageRequest;
 import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeDTO employeeDTO;
    private final String TEST_EMAIL = "employee@test.com";

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("Test Employee");
        employee.setEmail(TEST_EMAIL);
        employee.setPassword("securePass");
        employee.setBirthDate(LocalDate.of(1990, 1, 1));
        employee.setPhone("+380123456789");

        employeeDTO = new EmployeeDTO();
        employeeDTO.setName("Test Employee");
        employeeDTO.setEmail(TEST_EMAIL);
        employeeDTO.setPassword("securePass");
        employeeDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        employeeDTO.setPhone("+380123456789");
    }

    @Test
    void getAllEmployees_ShouldReturnPageOfEmployees() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> employeePage = new PageImpl<>(List.of(employee));

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);

        Page<EmployeeDTO> result = employeeService.getAllEmployees(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_EMAIL, result.getContent().get(0).getEmail());
        verify(employeeRepository, times(1)).findAll(pageable);
    }

    @Test
    void getEmployeeByEmail_ShouldReturnEmployee_WhenExists() {
        when(employeeRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(employee));
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);

        EmployeeDTO result = employeeService.getEmployeeByEmail(TEST_EMAIL);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    void getEmployeeByEmail_ShouldThrowNotFoundException_WhenDoesNotExist() {
        when(employeeRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.getEmployeeByEmail("unknown@test.com"));
    }

    @Test
    void addEmployee_ShouldSaveAndReturnEmployee() {
        // Налаштовуємо шифрування
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(modelMapper.map(employeeDTO, Employee.class)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);

        EmployeeDTO result = employeeService.addEmployee(employeeDTO);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        // Перевіряємо, що пароль шифрувався
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void updateEmployeeByEmail_ShouldUpdateAndReturnEmployee() {
        when(employeeRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_new_password");
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);

        EmployeeDTO result = employeeService.updateEmployeeByEmail(TEST_EMAIL, employeeDTO);

        assertNotNull(result);
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void deleteEmployeeByEmail_ShouldCallDeleteMethod() {
        doNothing().when(employeeRepository).deleteByEmail(TEST_EMAIL);

        employeeService.deleteEmployeeByEmail(TEST_EMAIL);

        verify(employeeRepository, times(1)).deleteByEmail(TEST_EMAIL);
    }
}