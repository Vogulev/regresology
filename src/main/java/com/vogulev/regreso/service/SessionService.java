package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CancelSessionRequest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.UpdateSessionRequest;
import com.vogulev.regreso.dto.response.SessionPrepResponse;
import com.vogulev.regreso.dto.response.SessionResponse;
import com.vogulev.regreso.dto.response.SessionSummaryStatusResponse;

import java.util.UUID;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request, UUID practitionerId);

    SessionResponse getSession(UUID sessionId, UUID practitionerId);

    SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId);

    SessionResponse completeSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId);

    SessionResponse cancelSession(UUID sessionId, CancelSessionRequest request, UUID practitionerId);

    SessionPrepResponse getSessionPrep(UUID sessionId, UUID practitionerId);

    SessionSummaryStatusResponse getSessionSummary(UUID sessionId, UUID practitionerId);

    void triggerSessionSummaryGeneration(UUID sessionId, UUID practitionerId);
}
