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

    @Mock private EmployeeRepository employeeRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeDTO employeeDTO;
    private final String testEmail = "employee@test.com";

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("Test Employee");
        employee.setEmail(testEmail);
        employee.setPassword("securePass");
        employee.setBirthDate(LocalDate.of(1990, 1, 1));
        employee.setPhone("+380123456789");

        employeeDTO = new EmployeeDTO();
        employeeDTO.setName("Test Employee");
        employeeDTO.setEmail(testEmail);
        employeeDTO.setPassword("securePass");
        employeeDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        employeeDTO.setPhone("+380123456789");
    }

    @Test
    void getAllEmployees_ShouldReturnPageOfEmployees() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> employeePage = new PageImpl<>(List.of(employee));

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(modelMapper.map(any(Employee.class), eq(EmployeeDTO.class))).thenReturn(employeeDTO);

        Page<EmployeeDTO> result = employeeService.getAllEmployees(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(employeeRepository).findAll(pageable);
    }

    @Test
    void getEmployeeByEmail_LogicTest() {
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);
        assertNotNull(employeeService.getEmployeeByEmail(testEmail));

        when(employeeRepository.findByEmail("none")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> employeeService.getEmployeeByEmail("none"));
    }

    @Test
    void addEmployee_ShouldSaveSuccessfully() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(modelMapper.map(any(EmployeeDTO.class), eq(Employee.class))).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(modelMapper.map(employee, EmployeeDTO.class)).thenReturn(employeeDTO);

        assertNotNull(employeeService.addEmployee(employeeDTO));
        verify(passwordEncoder).encode("securePass");
        verify(employeeRepository).save(employee);
    }

    @Test
    void deleteEmployeeByEmail_ShouldCallRepository() {
        doNothing().when(employeeRepository).deleteByEmail(testEmail);
        employeeService.deleteEmployeeByEmail(testEmail);
        verify(employeeRepository).deleteByEmail(testEmail);
    }

    @Test
    void updateEmployeeByEmail_WithPassword_ShouldUpdate() {
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));
        doNothing().when(modelMapper).map(any(EmployeeDTO.class), any(Employee.class));
        when(passwordEncoder.encode("securePass")).thenReturn("encoded_new");
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(modelMapper.map(any(Employee.class), eq(EmployeeDTO.class))).thenReturn(employeeDTO);

        employeeService.updateEmployeeByEmail(testEmail, employeeDTO);

        verify(passwordEncoder).encode("securePass");
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployeeByEmail_NullPassword_ShouldNotEncode() {
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));
        doNothing().when(modelMapper).map(any(EmployeeDTO.class), any(Employee.class));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(modelMapper.map(any(Employee.class), eq(EmployeeDTO.class))).thenReturn(employeeDTO);

        employeeDTO.setPassword(null);
        employeeService.updateEmployeeByEmail(testEmail, employeeDTO);

        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployeeByEmail_EmptyPassword_ShouldNotEncode() {
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));
        doNothing().when(modelMapper).map(any(EmployeeDTO.class), any(Employee.class));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(modelMapper.map(any(Employee.class), eq(EmployeeDTO.class))).thenReturn(employeeDTO);

        employeeDTO.setPassword("");
        employeeService.updateEmployeeByEmail(testEmail, employeeDTO);

        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository).save(employee);
    }

    @Test
    void updatePersonalData_ShouldUpdateAndSave() {
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));

        String newPhone = "+380999999999";
        LocalDate newBirthDate = LocalDate.of(1995, 5, 5);

        employeeService.updatePersonalData(testEmail, newPhone, newBirthDate);

        assertEquals(newPhone, employee.getPhone());
        assertEquals(newBirthDate, employee.getBirthDate());
        verify(employeeRepository).save(employee);
    }
}