package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;
    private ClientDTO clientDTO;
    private final String testEmail = "client@test.com";

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Test Client");
        client.setEmail(testEmail);
        client.setPassword("password123");
        client.setBalance(BigDecimal.valueOf(1000.0));

        clientDTO = new ClientDTO();
        clientDTO.setName("Test Client");
        clientDTO.setEmail(testEmail);
        clientDTO.setPassword("password123");
        clientDTO.setBalance(BigDecimal.valueOf(1000.0));
    }

    @Test
    void getAllClients_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(clientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(client)));
        when(modelMapper.map(any(Client.class), eq(ClientDTO.class))).thenReturn(clientDTO);

        Page<ClientDTO> result = clientService.getAllClients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getClientByEmail_LogicTest() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        when(modelMapper.map(client, ClientDTO.class)).thenReturn(clientDTO);
        assertNotNull(clientService.getClientByEmail(testEmail));

        when(clientRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> clientService.getClientByEmail("none@test.com"));
    }

    @Test
    void addClient_ShouldSaveSuccessfully() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(modelMapper.map(any(ClientDTO.class), eq(Client.class))).thenReturn(client);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(any(Client.class), eq(ClientDTO.class))).thenReturn(clientDTO);

        assertNotNull(clientService.addClient(clientDTO));
        verify(clientRepository).save(any(Client.class));
    }


    @Test
    void updateClient_ShouldDelegateToUpdateByEmail() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        doNothing().when(modelMapper).map(any(ClientDTO.class), any(Client.class));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(any(Client.class), eq(ClientDTO.class))).thenReturn(clientDTO);

        clientService.updateClient(testEmail, clientDTO);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void updateClientProfile_ShouldNotCheckEmail_WhenEmailIsUnchanged() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        doNothing().when(modelMapper).map(any(ClientDTO.class), any(Client.class));

        clientDTO.setEmail(testEmail);
        clientService.updateClientProfile(testEmail, clientDTO);

        verify(clientRepository, times(1)).save(client);
        verify(clientRepository, never()).findByEmail(argThat(e -> !e.equals(testEmail)));
    }

    @Test
    void updateClientProfile_ShouldUpdate_WhenNewEmailIsAvailable() {
        String newEmail = "new@test.com";
        clientDTO.setEmail(newEmail);
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        doNothing().when(modelMapper).map(any(ClientDTO.class), any(Client.class));

        clientService.updateClientProfile(testEmail, clientDTO);
        verify(clientRepository).save(client);
    }

    @Test
    void updateClientProfile_ShouldThrow_WhenEmailTaken() {
        String busyEmail = "busy@test.com";
        clientDTO.setEmail(busyEmail);
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail(busyEmail)).thenReturn(Optional.of(new Client()));

        assertThrows(AlreadyExistException.class, () -> clientService.updateClientProfile(testEmail, clientDTO));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClientProfile_ShouldThrow_WhenClientNotFound() {
        when(clientRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> clientService.updateClientProfile("missing@test.com", clientDTO));
    }

    @Test
    void addBalance_LogicBranchesTest() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));

        clientService.addBalance(testEmail, BigDecimal.valueOf(500));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(client.getBalance()));

        client.setBalance(null);
        clientService.addBalance(testEmail, BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(client.getBalance()));

        assertThrows(IllegalArgumentException.class, () -> clientService.addBalance(testEmail, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> clientService.addBalance(testEmail, new BigDecimal("-1.0")));

        when(clientRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> clientService.addBalance("none@test.com", BigDecimal.ONE));
    }

    @Test
    void deleteClientByEmail_ShouldCallRepository() {
        doNothing().when(clientRepository).deleteByEmail(testEmail);
        clientService.deleteClientByEmail(testEmail);
        verify(clientRepository).deleteByEmail(testEmail);
    }

    @Test
    void updateClientProfile_AvailableEmailChange() {
        String newEmail = "new_free@test.com";
        clientDTO.setEmail(newEmail);

        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.empty());

        clientService.updateClientProfile(testEmail, clientDTO);

        verify(clientRepository).save(client);
    }

    @Test
    void updateClientProfile_EmailChangeBranches() {
        String newEmail = "new@test.com";
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));
        doNothing().when(modelMapper).map(any(ClientDTO.class), any(Client.class));

        clientDTO.setEmail(newEmail);
        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        clientService.updateClientProfile(testEmail, clientDTO);
        verify(clientRepository, times(1)).save(client);

        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.of(new Client()));
        assertThrows(AlreadyExistException.class, () -> clientService.updateClientProfile(testEmail, clientDTO));
    }

    @Test
    void addBalance_TernaryCoverage() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));

        client.setBalance(null);
        clientService.addBalance(testEmail, BigDecimal.TEN);
        assertEquals(0, BigDecimal.TEN.compareTo(client.getBalance()));
    }

    @Test
    void addBalance_TernaryBranchCoverage() {
        when(clientRepository.findByEmail(testEmail)).thenReturn(Optional.of(client));

        client.setBalance(BigDecimal.valueOf(100));
        clientService.addBalance(testEmail, BigDecimal.valueOf(50));
        assertEquals(0, BigDecimal.valueOf(150).compareTo(client.getBalance()));

        client.setBalance(null);
        clientService.addBalance(testEmail, BigDecimal.valueOf(50));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(client.getBalance()));
    }

}