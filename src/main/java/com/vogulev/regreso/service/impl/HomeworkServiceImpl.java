package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.CreateHomeworkRequest;
import com.vogulev.regreso.dto.response.HomeworkResponse;
import com.vogulev.regreso.entity.*;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.*;
import com.vogulev.regreso.service.HomeworkService;
import com.vogulev.regreso.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HomeworkServiceImpl implements HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final PractitionerRepository practitionerRepository;
    private final MaterialRepository materialRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponse> getClientHomework(UUID clientId, UUID practitionerId) {
        checkClientAccess(clientId, practitionerId);
        return homeworkRepository
                .findByClientIdAndPractitionerIdOrderByCreatedAtDesc(clientId, practitionerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HomeworkResponse createForClient(UUID clientId, CreateHomeworkRequest request, UUID practitionerId) {
        Client client = clientRepository.findByIdAndPractitionerId(clientId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        Homework homework = buildHomework(request, practitioner, client, null);
        homework = homeworkRepository.save(homework);
        telegramNotificationService.sendHomeworkNotification(client, homework);
        return toResponse(homework);
    }

    @Override
    public HomeworkResponse createForSession(UUID sessionId, CreateHomeworkRequest request, UUID practitionerId) {
        Session session = sessionRepository.findByIdAndPractitionerId(sessionId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Сессия не найдена"));
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        Homework homework = buildHomework(request, practitioner, session.getClient(), session);
        homework = homeworkRepository.save(homework);
        telegramNotificationService.sendHomeworkNotification(session.getClient(), homework);
        return toResponse(homework);
    }

    @Override
    public HomeworkResponse updateStatus(UUID id, String status, UUID practitionerId) {
        Homework homework = homeworkRepository.findByIdAndPractitionerId(id, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Задание не найдено"));

        Homework.Status newStatus;
        try {
            newStatus = Homework.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Недопустимый статус: " + status + ". Допустимые: COMPLETED, SKIPPED");
        }

        if (newStatus != Homework.Status.COMPLETED && newStatus != Homework.Status.SKIPPED) {
            throw new BusinessException("Допустимые статусы: COMPLETED, SKIPPED");
        }

        homework.setStatus(newStatus);
        return toResponse(homework);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Homework buildHomework(CreateHomeworkRequest request, Practitioner practitioner,
                                    Client client, Session session) {
        PractitionerMaterial material = null;
        if (request.getMaterialId() != null) {
            material = materialRepository.findByIdAndPractitionerId(request.getMaterialId(), practitioner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
        }

        String title = resolveTitle(request.getTitle(), material);
        String description = resolveDescription(request.getDescription(), material);

        if (title == null || title.isBlank()) {
            throw new BusinessException("Необходимо указать title или materialId с заголовком");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Необходимо указать description или materialId с описанием");
        }

        Homework.Type type = null;
        if (request.getHomeworkType() != null && !request.getHomeworkType().isBlank()) {
            try {
                type = Homework.Type.valueOf(request.getHomeworkType());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Недопустимый тип задания: " + request.getHomeworkType());
            }
        }

        return Homework.builder()
                .client(client)
                .practitioner(practitioner)
                .session(session)
                .material(material)
                .title(title)
                .description(description)
                .homeworkType(type)
                .dueDate(request.getDueDate())
                .build();
    }

    private String resolveTitle(String requestTitle, PractitionerMaterial material) {
        if (requestTitle != null && !requestTitle.isBlank()) return requestTitle;
        if (material != null) return material.getTitle();
        return null;
    }

    private String resolveDescription(String requestDescription, PractitionerMaterial material) {
        if (requestDescription != null && !requestDescription.isBlank()) return requestDescription;
        if (material != null) return material.getDescription();
        return null;
    }

    private void checkClientAccess(UUID clientId, UUID practitionerId) {
        clientRepository.findByIdAndPractitionerId(clientId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
    }

    private HomeworkResponse toResponse(Homework hw) {
        return HomeworkResponse.builder()
                .id(hw.getId())
                .sessionId(hw.getSession() != null ? hw.getSession().getId() : null)
                .clientId(hw.getClient().getId())
                .clientFullName(hw.getClient().getFullName())
                .materialId(hw.getMaterial() != null ? hw.getMaterial().getId() : null)
                .title(hw.getTitle())
                .description(hw.getDescription())
                .homeworkType(hw.getHomeworkType() != null ? hw.getHomeworkType().name() : null)
                .dueDate(hw.getDueDate())
                .status(hw.getStatus() != null ? hw.getStatus().name() : null)
                .clientResponse(hw.getClientResponse())
                .respondedAt(hw.getRespondedAt())
                .createdAt(hw.getCreatedAt())
                .build();
    }
}
