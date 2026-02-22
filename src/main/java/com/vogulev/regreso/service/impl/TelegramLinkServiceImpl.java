package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.service.TelegramLinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TelegramLinkServiceImpl implements TelegramLinkService {

    @Value("${telegram.bot.username:RegresoBot}")
    private String botUsername;

    @Override
    public String generateClientLink(UUID clientId) {
        return "https://t.me/" + botUsername + "?start=CLIENT_" + clientId;
    }

    @Override
    public String generatePractitionerLink(UUID practitionerId) {
        return "https://t.me/" + botUsername + "?start=PRACTITIONER_" + practitionerId;
    }
}
