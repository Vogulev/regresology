package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CreateThemeRequest;
import com.vogulev.regreso.dto.request.LinkSessionToThemeRequest;
import com.vogulev.regreso.dto.response.ClientThemeResponse;

import java.util.List;
import java.util.UUID;

public interface ClientThemeService {

    List<ClientThemeResponse> getClientThemes(UUID clientId, UUID practitionerId);

    ClientThemeResponse createTheme(UUID clientId, CreateThemeRequest request, UUID practitionerId);

    ClientThemeResponse linkSession(UUID themeId, LinkSessionToThemeRequest request, UUID practitionerId);

    ClientThemeResponse resolveTheme(UUID themeId, UUID practitionerId);
}
