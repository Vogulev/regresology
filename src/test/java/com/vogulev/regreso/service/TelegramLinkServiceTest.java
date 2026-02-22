package com.vogulev.regreso.service;

import com.vogulev.regreso.service.impl.TelegramLinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelegramLinkService — юнит-тесты")
class TelegramLinkServiceTest {

    private TelegramLinkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelegramLinkServiceImpl();
        ReflectionTestUtils.setField(service, "botUsername", "RegresoBot");
    }

    @Test
    @DisplayName("generateClientLink → корректный формат ссылки")
    void generateClientLink_correctFormat() {
        UUID clientId = UUID.randomUUID();
        String link = service.generateClientLink(clientId);
        assertThat(link).isEqualTo("https://t.me/RegresoBot?start=CLIENT_" + clientId);
    }

    @Test
    @DisplayName("generatePractitionerLink → корректный формат ссылки")
    void generatePractitionerLink_correctFormat() {
        UUID practitionerId = UUID.randomUUID();
        String link = service.generatePractitionerLink(practitionerId);
        assertThat(link).isEqualTo("https://t.me/RegresoBot?start=PRACTITIONER_" + practitionerId);
    }

    @Test
    @DisplayName("ссылки содержат корректные префиксы")
    void links_haveCorrectPrefixes() {
        UUID id = UUID.randomUUID();
        assertThat(service.generateClientLink(id)).contains("CLIENT_");
        assertThat(service.generatePractitionerLink(id)).contains("PRACTITIONER_");
    }

    @Test
    @DisplayName("разные ID → разные ссылки")
    void differentIds_differentLinks() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        assertThat(service.generateClientLink(id1)).isNotEqualTo(service.generateClientLink(id2));
    }
}
