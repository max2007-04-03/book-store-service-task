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
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;
    private ClientDTO clientDTO;
    private final String TEST_EMAIL = "client@test.com";

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Test Client");
        client.setEmail(TEST_EMAIL);
        client.setPassword("password123");
        client.setBalance(BigDecimal.valueOf(1000.0));

        clientDTO = new ClientDTO();
        clientDTO.setName("Test Client");
        clientDTO.setEmail(TEST_EMAIL);
        clientDTO.setPassword("password123");
        clientDTO.setBalance(BigDecimal.valueOf(1000.0));
    }


    @Test
    void getAllClients_ShouldReturnPageOfClients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientPage = new PageImpl<>(List.of(client));

        when(clientRepository.findAll(pageable)).thenReturn(clientPage);
        when(modelMapper.map(client, ClientDTO.class)).thenReturn(clientDTO);

        Page<ClientDTO> result = clientService.getAllClients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_EMAIL, result.getContent().get(0).getEmail());
        verify(clientRepository, times(1)).findAll(pageable);
    }


    @Test
    void getClientByEmail_ShouldReturnClient_WhenExists() {
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));
        when(modelMapper.map(client, ClientDTO.class)).thenReturn(clientDTO);

        ClientDTO result = clientService.getClientByEmail(TEST_EMAIL);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    void getClientByEmail_ShouldThrowNotFoundException_WhenDoesNotExist() {
        when(clientRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clientService.getClientByEmail("unknown@test.com"));
    }


    @Test
    void addClient_ShouldSaveAndReturnClient() {
        ClientDTO dto = new ClientDTO();
        dto.setPassword("raw_password");
        Client clientToSave = new Client();

        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(modelMapper.map(dto, Client.class)).thenReturn(clientToSave);
        when(clientRepository.save(clientToSave)).thenReturn(clientToSave);
        when(modelMapper.map(clientToSave, ClientDTO.class)).thenReturn(dto);

        ClientDTO result = clientService.addClient(dto);

        assertNotNull(result);
        verify(passwordEncoder, times(1)).encode("raw_password");
        verify(clientRepository, times(1)).save(clientToSave);
    }


    @Test
    void updateClientByEmail_ShouldUpdateAndReturnClient() {
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(client, ClientDTO.class)).thenReturn(clientDTO);

        clientDTO.setName("Updated Name");
        ClientDTO result = clientService.updateClientByEmail(TEST_EMAIL, clientDTO);

        assertNotNull(result);
        verify(clientRepository, times(1)).save(client);
    }

    @Test
    void updateClientByEmail_ShouldThrowNotFoundException_WhenDoesNotExist() {
        when(clientRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clientService.updateClientByEmail("unknown@test.com", clientDTO));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateClient_ShouldCallUpdateClientByEmail() {
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(modelMapper.map(client, ClientDTO.class)).thenReturn(clientDTO);

        clientService.updateClient(TEST_EMAIL, clientDTO);

        verify(clientRepository, times(1)).save(client);
    }


    @Test
    void deleteClientByEmail_ShouldCallDeleteMethod() {
        doNothing().when(clientRepository).deleteByEmail(TEST_EMAIL);

        clientService.deleteClientByEmail(TEST_EMAIL);

        verify(clientRepository, times(1)).deleteByEmail(TEST_EMAIL);
    }


    @Test
    void updateClientProfile_ShouldUpdateProfile_WhenEmailIsUnchanged() {
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));

        clientDTO.setName("New Name"); // Змінюємо ім'я, але залишаємо той самий email

        clientService.updateClientProfile(TEST_EMAIL, clientDTO);

        verify(clientRepository, times(1)).save(client);
        assertEquals("New Name", client.getName());
    }

    @Test
    void updateClientProfile_ShouldUpdateProfile_WhenNewEmailIsAvailable() {
        String newEmail = "new@test.com";
        clientDTO.setEmail(newEmail);

        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.empty());

        clientService.updateClientProfile(TEST_EMAIL, clientDTO);

        verify(clientRepository, times(1)).save(client);
        assertEquals(newEmail, client.getEmail());
    }

    @Test
    void updateClientProfile_ShouldThrowAlreadyExistException_WhenNewEmailIsTaken() {
        String newEmail = "new@test.com";
        clientDTO.setEmail(newEmail);

        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));
        // Імітуємо, що новий email вже зайнятий іншим користувачем
        when(clientRepository.findByEmail(newEmail)).thenReturn(Optional.of(new Client()));

        assertThrows(AlreadyExistException.class, () -> clientService.updateClientProfile(TEST_EMAIL, clientDTO));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateClientProfile_ShouldThrowNotFoundException_WhenClientDoesNotExist() {
        when(clientRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clientService.updateClientProfile("unknown@test.com", clientDTO));
        verify(clientRepository, never()).save(any(Client.class));
    }


    @Test
    void addBalance_ShouldIncreaseBalance_WhenAmountIsPositive() {
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));

        BigDecimal amountToAdd = BigDecimal.valueOf(500.0);
        clientService.addBalance(TEST_EMAIL, amountToAdd);

        verify(clientRepository, times(1)).save(client);
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(client.getBalance()));
    }

    @Test
    void addBalance_ShouldSetBalance_WhenCurrentBalanceIsNull() {
        client.setBalance(null);
        when(clientRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(client));

        BigDecimal amountToAdd = BigDecimal.valueOf(500.0);
        clientService.addBalance(TEST_EMAIL, amountToAdd);

        verify(clientRepository, times(1)).save(client);
        assertEquals(amountToAdd, client.getBalance());
    }

    @Test
    void addBalance_ShouldThrowIllegalArgumentException_WhenAmountIsZeroOrLess() {
        assertThrows(IllegalArgumentException.class, () -> clientService.addBalance(TEST_EMAIL, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> clientService.addBalance(TEST_EMAIL, BigDecimal.valueOf(-100.0)));
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void addBalance_ShouldThrowRuntimeException_WhenClientDoesNotExist() {
        when(clientRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> clientService.addBalance("unknown@test.com", BigDecimal.valueOf(100.0)));
        verify(clientRepository, never()).save(any(Client.class));
    }
}