package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public Page<ClientDTO> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(client -> modelMapper.map(client, ClientDTO.class));
    }

    @Override
    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Клієнта з email " + email + " не знайдено"));
        return modelMapper.map(client, ClientDTO.class);
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        Client client = modelMapper.map(clientDTO, Client.class);
        client.setPassword(passwordEncoder.encode(clientDTO.getPassword()));
        Client savedClient = clientRepository.save(client);
        return modelMapper.map(savedClient, ClientDTO.class);
    }

    @Override
    @Transactional
    public ClientDTO updateClientByEmail(String email, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Клієнта не знайдено"));

        modelMapper.map(clientDTO, existingClient);

        Client updatedClient = clientRepository.save(existingClient);
        return modelMapper.map(updatedClient, ClientDTO.class);
    }

    @Override
    @Transactional
    public void deleteClientByEmail(String email) {
        clientRepository.deleteByEmail(email);
    }

    @Override
    @Transactional
    public void updateClient(String email, ClientDTO clientDTO) {
        updateClientByEmail(email, clientDTO);
    }

    @Override
    @Transactional
    public void updateClientProfile(String currentEmail, ClientDTO updatedData) {
        Client client = clientRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new NotFoundException("Клієнта не знайдено"));

        if (!currentEmail.equals(updatedData.getEmail())) {
            if (clientRepository.findByEmail(updatedData.getEmail()).isPresent()) {
                throw new AlreadyExistException("Користувач з поштою " + updatedData.getEmail() + " вже існує");
            }
        }

        modelMapper.map(updatedData, client);

        clientRepository.save(client);
    }

    @Override
    @Transactional
    public void addBalance(String email, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сума поповнення має бути більшою за нуль");
        }

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Клієнта не знайдено"));

        BigDecimal currentBalance = client.getBalance() != null ? client.getBalance() : BigDecimal.ZERO;
        client.setBalance(currentBalance.add(amount));

        clientRepository.save(client);
    }
}