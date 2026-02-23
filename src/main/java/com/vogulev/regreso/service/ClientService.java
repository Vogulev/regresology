package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.ClientRequest;
import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.dto.response.SessionListItemResponse;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    List<ClientListItemResponse> getClients(UUID practitionerId);

    ClientResponse getClient(UUID id, UUID practitionerId);

    ClientResponse createClient(ClientRequest request, UUID practitionerId);

    ClientResponse updateClient(UUID id, ClientRequest request, UUID practitionerId);

    void archiveClient(UUID id, boolean archive, UUID practitionerId);

    void updateProgress(UUID id, String overallProgress, UUID practitionerId);

    List<SessionListItemResponse> getClientSessions(UUID clientId, UUID practitionerId);

    void triggerClientSummaryGeneration(UUID clientId, UUID practitionerId);
}
