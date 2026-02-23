package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.ai.AiSummaryService;
import com.vogulev.regreso.dto.request.ClientRequest;
import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.dto.response.SessionListItemResponse;
import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.mapper.ClientMapper;
import com.vogulev.regreso.mapper.SessionMapper;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.HomeworkRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.repository.SessionRepository;
import com.vogulev.regreso.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final HomeworkRepository homeworkRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClientMapper clientMapper;
    private final SessionMapper sessionMapper;
    private final AiSummaryService aiSummaryService;

    @Override
    @Transactional(readOnly = true)
    public List<ClientListItemResponse> getClients(UUID practitionerId) {
        return clientRepository
                .findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(practitionerId)
                .stream()
                .map(this::toListItemWithStats)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID id, UUID practitionerId) {
        Client client = findClientOrThrow(id, practitionerId);
        return toResponseWithStats(client);
    }

    @Override
    public ClientResponse createClient(ClientRequest request, UUID practitionerId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        Client client = Client.builder()
                .practitioner(practitioner)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .telegramUsername(request.getTelegramUsername())
                .initialRequest(request.getInitialRequest())
                .presentingIssues(toArray(request.getPresentingIssues()))
                .hasContraindications(request.getHasContraindications() != null
                        ? request.getHasContraindications() : false)
                .contraindicationsNotes(request.getContraindicationsNotes())
                .intakeFormCompleted(request.getIntakeFormCompleted() != null
                        ? request.getIntakeFormCompleted() : false)
                .generalNotes(request.getGeneralNotes())
                .build();

        client = clientRepository.save(client);
        return toResponseWithStats(client);
    }

    @Override
    public ClientResponse updateClient(UUID id, ClientRequest request, UUID practitionerId) {
        Client client = findClientOrThrow(id, practitionerId);

        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setBirthDate(request.getBirthDate());
        client.setTelegramUsername(request.getTelegramUsername());
        client.setInitialRequest(request.getInitialRequest());
        client.setPresentingIssues(toArray(request.getPresentingIssues()));
        if (request.getHasContraindications() != null) {
            client.setHasContraindications(request.getHasContraindications());
        }
        client.setContraindicationsNotes(request.getContraindicationsNotes());
        if (request.getIntakeFormCompleted() != null) {
            client.setIntakeFormCompleted(request.getIntakeFormCompleted());
        }
        client.setGeneralNotes(request.getGeneralNotes());

        return toResponseWithStats(client);
    }

    @Override
    public void archiveClient(UUID id, boolean archive, UUID practitionerId) {
        Client client = findClientOrThrow(id, practitionerId);
        client.setIsArchived(archive);
    }

    @Override
    public void updateProgress(UUID id, String overallProgress, UUID practitionerId) {
        Client client = findClientOrThrow(id, practitionerId);
        client.setOverallProgress(overallProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionListItemResponse> getClientSessions(UUID clientId, UUID practitionerId) {
        findClientOrThrow(clientId, practitionerId);
        return sessionRepository.findByClientIdOrderByScheduledAtDesc(clientId).stream()
                .map(sessionMapper::toListItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void triggerClientSummaryGeneration(UUID clientId, UUID practitionerId) {
        aiSummaryService.triggerClientSummary(clientId, practitionerId);
    }

    private Client findClientOrThrow(UUID id, UUID practitionerId) {
        return clientRepository.findByIdAndPractitionerId(id, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
    }

    private ClientResponse toResponseWithStats(Client client) {
        ClientResponse response = clientMapper.toResponse(client);
        fillStats(response, client.getId());
        return response;
    }

    private ClientListItemResponse toListItemWithStats(Client client) {
        ClientListItemResponse response = clientMapper.toListItemResponse(client);
        fillListStats(response, client.getId());
        return response;
    }

    private void fillStats(ClientResponse response, UUID clientId) {
        response.setTotalSessions((int) sessionRepository.countByClientId(clientId));
        response.setCompletedSessions(
                (int) sessionRepository.countByClientIdAndStatus(clientId, Session.Status.COMPLETED));
        response.setLastSessionAt(
                sessionRepository.findFirstByClientIdOrderByScheduledAtDesc(clientId)
                        .map(Session::getScheduledAt)
                        .orElse(null));
        response.setActiveHomeworkCount(
                (int) homeworkRepository.countByClientIdAndStatus(clientId, Homework.Status.ASSIGNED));
    }

    private void fillListStats(ClientListItemResponse response, UUID clientId) {
        response.setTotalSessions((int) sessionRepository.countByClientId(clientId));
        response.setLastSessionAt(
                sessionRepository.findFirstByClientIdOrderByScheduledAtDesc(clientId)
                        .map(Session::getScheduledAt)
                        .orElse(null));
        response.setActiveHomeworkCount(
                (int) homeworkRepository.countByClientIdAndStatus(clientId, Homework.Status.ASSIGNED));
    }

    private String[] toArray(List<String> list) {
        return list != null ? list.toArray(String[]::new) : null;
    }
}
