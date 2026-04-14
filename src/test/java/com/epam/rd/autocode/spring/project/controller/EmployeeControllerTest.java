package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.InvalidProfileDataException;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EmployeeController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAllEmployees_ShouldReturnListView() throws Exception {
        List<EmployeeDTO> employees = List.of(new EmployeeDTO());
        org.springframework.data.domain.Page<EmployeeDTO> employeePage =
                new org.springframework.data.domain.PageImpl<>(employees);

        when(employeeService.getAllEmployees(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(employeePage);

        mockMvc.perform(get("/employees").param("sortBy", "email"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(model().attributeExists("employeePage", "currentPage", "totalPages", "employees"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getEmployeeDetails_ShouldReturnDetailView() throws Exception {
        String email = "emp@store.com";
        when(employeeService.getEmployeeByEmail(email)).thenReturn(new EmployeeDTO());

        mockMvc.perform(get("/employees/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/detail"))
                .andExpect(model().attributeExists("employee"));
    }

    @Test
    @WithMockUser(username = "emp@store.com", roles = "EMPLOYEE")
    void editEmployeeForm_ShouldReturnEditView_WhenPrincipalMatches() throws Exception {
        String email = "emp@store.com";
        when(employeeService.getEmployeeByEmail(email)).thenReturn(new EmployeeDTO());

        mockMvc.perform(get("/employees/" + email + "/edit").principal(() -> email))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/employee-edit"))
                .andExpect(model().attributeExists("employee"));
    }

    @Test
    @WithMockUser(username = "other@store.com", roles = "EMPLOYEE")
    void editEmployeeForm_ShouldRedirect_WhenPrincipalDoesNotMatch() throws Exception {
        mockMvc.perform(get("/employees/emp@store.com/edit").principal(() -> "other@store.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
    }

    @Test
    void editEmployeeForm_ShouldRedirect_WhenPrincipalIsNull() throws Exception {
        mockMvc.perform(get("/employees/emp@store.com/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldRedirect_WhenDataIsValid() throws Exception {
        String email = "emp@store.com";

        mockMvc.perform(post("/employees/" + email + "/update")
                        .with(csrf())
                        .param("phone", "+380123456789")
                        .param("birthDate", "1990-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/" + email));

        verify(employeeService, times(1)).updatePersonalData(eq(email), eq("+380123456789"), eq(LocalDate.of(1990, 1, 1)));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldRedirect_WhenDataIsEmpty() throws Exception {
        String email = "emp@store.com";

        mockMvc.perform(post("/employees/" + email + "/update")
                        .with(csrf())
                        .param("phone", "")
                        .param("birthDate", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/" + email));

        verify(employeeService, times(1)).updatePersonalData(eq(email), eq(""), eq(null));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldThrowException_WhenPhoneContainsLetters() throws Exception {
        mockMvc.perform(post("/employees/emp@store.com/update").with(csrf()).param("phone", "+380abc456789"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldThrowException_WhenPhoneIsTooShort() throws Exception {
        mockMvc.perform(post("/employees/emp@store.com/update").with(csrf()).param("phone", "+3801234"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldThrowException_WhenPhoneIsTooLong() throws Exception {
        mockMvc.perform(post("/employees/emp@store.com/update").with(csrf()).param("phone", "+38012345678901234"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldThrowException_WhenBirthDateIsInFuture() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/employees/emp@store.com/update").with(csrf()).param("birthDate", futureDate))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_ShouldThrowException_WhenBirthDateIsTooOld() throws Exception {
        String oldDate = LocalDate.now().minusYears(121).toString();
        mockMvc.perform(post("/employees/emp@store.com/update").with(csrf()).param("birthDate", oldDate))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));
    }

    @Test
    @WithMockUser(username = "admin@store.com", roles = "EMPLOYEE")
    void deleteEmployee_ShouldRedirect_WhenDeletingOtherEmployee() throws Exception {
        String email = "emp@store.com";
        doNothing().when(employeeService).deleteEmployeeByEmail(email);

        mockMvc.perform(post("/employees/delete")
                        .principal(() -> "admin@store.com")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(employeeService, times(1)).deleteEmployeeByEmail(email);
    }

    @Test
    @WithMockUser(username = "admin@store.com", roles = "EMPLOYEE")
    void deleteEmployee_ShouldRedirectAndShowError_WhenDeletingSelf() throws Exception {
        String email = "admin@store.com";

        mockMvc.perform(post("/employees/delete")
                        .principal(() -> email)
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(employeeService, never()).deleteEmployeeByEmail(anyString());
    }
}