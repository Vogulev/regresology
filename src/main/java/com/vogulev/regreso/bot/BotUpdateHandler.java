package com.vogulev.regreso.bot;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.HomeworkRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Бизнес-логика обработки входящих Telegram-обновлений.
 * Не вызывает Telegram API напрямую — возвращает текст ответа (если нужен).
 * Это делает класс тестируемым без реального бота.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotUpdateHandler {

    private final ClientRepository clientRepository;
    private final HomeworkRepository homeworkRepository;
    private final PractitionerRepository practitionerRepository;

    @Transactional
    public Optional<String> handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return Optional.empty();
        }

        Message message = update.getMessage();
        String text = message.getText().trim();
        Long chatId = message.getChatId();

        if (text.startsWith("/start")) {
            return handleStart(text, chatId);
        }
        return handleTextMessage(text, chatId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Optional<String> handleStart(String text, Long chatId) {
        String param = text.length() > 7 ? text.substring(7).trim() : "";

        if (param.startsWith("CLIENT_")) {
            return handleClientStart(param.substring("CLIENT_".length()), chatId);
        }
        if (param.startsWith("PRACTITIONER_")) {
            return handlePractitionerStart(param.substring("PRACTITIONER_".length()), chatId);
        }

        return Optional.of("Добро пожаловать! Используйте ссылку от вашего специалиста для подключения.");
    }

    private Optional<String> handleClientStart(String clientIdStr, Long chatId) {
        UUID clientId;
        try {
            clientId = UUID.fromString(clientIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный clientId в /start: {}", clientIdStr);
            return Optional.of("Некорректная ссылка. Попросите специалиста выслать новую.");
        }

        return clientRepository.findById(clientId).map(client -> {
            client.setTelegramChatId(chatId);
            clientRepository.save(client);
            log.info("Telegram chat_id {} сохранён для клиента {}", chatId, clientId);
            return "Отлично! Теперь вы будете получать напоминания о сессиях";
        }).or(() -> {
            log.warn("Клиент {} не найден при /start", clientId);
            return Optional.of("Ссылка недействительна. Обратитесь к специалисту.");
        });
    }

    private Optional<String> handlePractitionerStart(String practitionerIdStr, Long chatId) {
        UUID practitionerId;
        try {
            practitionerId = UUID.fromString(practitionerIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный practitionerId в /start: {}", practitionerIdStr);
            return Optional.of("Некорректная ссылка.");
        }

        return practitionerRepository.findById(practitionerId).map(practitioner -> {
            practitioner.setTelegramChatId(chatId);
            practitionerRepository.save(practitioner);
            log.info("Telegram chat_id {} сохранён для практика {}", chatId, practitionerId);
            return "Бот подключён. Вы будете получать уведомления о клиентах и сессиях";
        }).or(() -> {
            log.warn("Практик {} не найден при /start", practitionerId);
            return Optional.of("Ссылка недействительна.");
        });
    }

    private Optional<String> handleTextMessage(String text, Long chatId) {
        Optional<Client> clientOpt = clientRepository.findByTelegramChatId(chatId);
        if (clientOpt.isEmpty()) {
            log.debug("Входящее сообщение от неизвестного chat_id {}, игнорируем", chatId);
            return Optional.empty();
        }

        Client client = clientOpt.get();
        Optional<Homework> homeworkOpt = homeworkRepository
                .findFirstByClientIdAndStatusOrderByCreatedAtDesc(client.getId(), Homework.Status.ASSIGNED);

        if (homeworkOpt.isEmpty()) {
            return Optional.of("Сейчас нет активных заданий");
        }

        Homework homework = homeworkOpt.get();
        homework.setClientResponse(text);
        homework.setRespondedAt(OffsetDateTime.now());
        homework.setStatus(Homework.Status.COMPLETED);
        homeworkRepository.save(homework);

        log.info("Ответ на задание {} сохранён от клиента {}", homework.getId(), client.getId());
        return Optional.of("Ответ сохранён ✓ Ваш специалист увидит его в карточке");
    }
}
