package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CreateHomeworkRequest;
import com.vogulev.regreso.dto.response.HomeworkResponse;

import java.util.List;
import java.util.UUID;

public interface HomeworkService {

    List<HomeworkResponse> getClientHomework(UUID clientId, UUID practitionerId);

    HomeworkResponse createForClient(UUID clientId, CreateHomeworkRequest request, UUID practitionerId);

    HomeworkResponse createForSession(UUID sessionId, CreateHomeworkRequest request, UUID practitionerId);

    HomeworkResponse updateStatus(UUID id, String status, UUID practitionerId);
}
