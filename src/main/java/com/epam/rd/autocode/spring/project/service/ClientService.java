package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;


public interface ClientService {

    Page<ClientDTO> getAllClients(Pageable pageable);

    ClientDTO getClientByEmail(String email);

    ClientDTO updateClientByEmail(String email, ClientDTO client);

    void deleteClientByEmail(String email);

    ClientDTO addClient(ClientDTO client);

    void updateClient(String email, ClientDTO clientDTO);

    void updateClientProfile(String currentEmail, ClientDTO updatedData);

    void addBalance(String email, BigDecimal amount);




}
