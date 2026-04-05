package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll().stream()
                .map(client -> modelMapper.map(client, ClientDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Клієнта з email " + email + " не знайдено"));
        return modelMapper.map(client, ClientDTO.class);
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        Client client = modelMapper.map(clientDTO, Client.class);
        Client savedClient = clientRepository.save(client);
        return modelMapper.map(savedClient, ClientDTO.class);
    }

    @Override
    @Transactional
    public ClientDTO updateClientByEmail(String email, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Клієнта не знайдено"));

        existingClient.setName(clientDTO.getName());
        existingClient.setPassword(clientDTO.getPassword());
        existingClient.setBalance(clientDTO.getBalance());

        Client updatedClient = clientRepository.save(existingClient);
        return modelMapper.map(updatedClient, ClientDTO.class);
    }

    @Override
    @Transactional
    public void deleteClientByEmail(String email) {
        clientRepository.deleteByEmail(email);
    }
}