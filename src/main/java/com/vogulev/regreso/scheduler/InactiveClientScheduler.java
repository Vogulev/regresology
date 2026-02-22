package com.vogulev.regreso.scheduler;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveClientScheduler {

    private final PractitionerRepository practitionerRepository;
    private final ClientRepository clientRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void notifyAboutInactiveClients() {
        List<Practitioner> practitioners = practitionerRepository.findAll();

        for (Practitioner practitioner : practitioners) {
            int days = practitioner.getInactiveClientReminderDays() != null
                    ? practitioner.getInactiveClientReminderDays() : 0;

            if (days == 0 || practitioner.getTelegramChatId() == null) continue;

            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days);
            List<Client> inactiveClients = clientRepository
                    .findInactiveClients(practitioner.getId(), cutoff);

            for (Client client : inactiveClients) {
                long daysSince = estimateDaysSinceLastSession(client, cutoff, days);
                telegramNotificationService.sendPractitionerInactiveClientAlert(
                        practitioner, client, daysSince);
            }

            if (!inactiveClients.isEmpty()) {
                log.info("Отправлено {} уведомлений о неактивных клиентах практику {}",
                        inactiveClients.size(), practitioner.getId());
            }
        }
    }

    private long estimateDaysSinceLastSession(Client client, OffsetDateTime cutoff, int configuredDays) {
        // Минимальная оценка: клиент неактивен как минимум configuredDays
        return configuredDays;
    }
}
