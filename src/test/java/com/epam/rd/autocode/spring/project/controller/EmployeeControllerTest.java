package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.CustomUserDetailsService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 1. Відключаємо Thymeleaf для тестів
@WebMvcTest(controllers = EmployeeController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    // 2. Заглушки для Security/JWT
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

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(model().attributeExists("employeePage", "currentPage", "totalPages"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteEmployee_ShouldRedirect() throws Exception {
        String email = "emp@store.com";
        doNothing().when(employeeService).deleteEmployeeByEmail(email);

        // 3. Додаємо .with(csrf()) до POST-запиту
        mockMvc.perform(post("/employees/delete")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(employeeService, times(1)).deleteEmployeeByEmail(email);
    }
}