package com.vogulev.regreso.service;

import java.util.UUID;

public interface TelegramLinkService {
    String generateClientLink(UUID clientId);
    String generatePractitionerLink(UUID practitionerId);
}
