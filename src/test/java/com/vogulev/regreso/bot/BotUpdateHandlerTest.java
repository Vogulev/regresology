package com.vogulev.regreso.bot;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.HomeworkRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BotUpdateHandler — юнит-тесты")
class BotUpdateHandlerTest {

    @Mock private ClientRepository clientRepository;
    @Mock private HomeworkRepository homeworkRepository;
    @Mock private PractitionerRepository practitionerRepository;

    @InjectMocks
    private BotUpdateHandler handler;

    private static final Long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {}

    // ── /start CLIENT_ ────────────────────────────────────────────────────────

    @Test
    @DisplayName("/start CLIENT_{uuid} → сохраняет telegramChatId клиента")
    void startWithClientId_savesChatId() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).firstName("Иван").build();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        Update update = buildTextUpdate("/start CLIENT_" + clientId, CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("напоминания");
        verify(clientRepository).save(client);
        assertThat(client.getTelegramChatId()).isEqualTo(CHAT_ID);
    }

    @Test
    @DisplayName("/start CLIENT_{неверный uuid} → сообщение об ошибке, без сохранения")
    void startWithInvalidClientId_returnsError() {
        Update update = buildTextUpdate("/start CLIENT_not-a-uuid", CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).containsIgnoringCase("некорректн");
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("/start CLIENT_{несуществующий} → сообщение об ошибке")
    void startWithUnknownClientId_returnsError() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        Update update = buildTextUpdate("/start CLIENT_" + clientId, CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).containsIgnoringCase("недействит");
    }

    // ── /start PRACTITIONER_ ──────────────────────────────────────────────────

    @Test
    @DisplayName("/start PRACTITIONER_{uuid} → сохраняет telegramChatId практика")
    void startWithPractitionerId_savesChatId() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = Practitioner.builder().id(practitionerId).firstName("Анна").build();
        when(practitionerRepository.findById(practitionerId)).thenReturn(Optional.of(practitioner));

        Update update = buildTextUpdate("/start PRACTITIONER_" + practitionerId, CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("подключён");
        verify(practitionerRepository).save(practitioner);
        assertThat(practitioner.getTelegramChatId()).isEqualTo(CHAT_ID);
    }

    // ── /start без параметра ──────────────────────────────────────────────────

    @Test
    @DisplayName("/start без параметра → приветственное сообщение")
    void startWithoutParam_returnsWelcome() {
        Update update = buildTextUpdate("/start", CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("ссылку");
    }

    // ── Ответ на задание ──────────────────────────────────────────────────────

    @Test
    @DisplayName("текстовое сообщение от клиента с заданием → сохраняет ответ")
    void textMessage_withActiveHomework_savesResponse() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).firstName("Иван").build();
        Homework homework = Homework.builder()
                .id(UUID.randomUUID())
                .title("Медитация")
                .status(Homework.Status.ASSIGNED)
                .build();

        when(clientRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(client));
        when(homeworkRepository.findFirstByClientIdAndStatusOrderByCreatedAtDesc(clientId, Homework.Status.ASSIGNED))
                .thenReturn(Optional.of(homework));

        Update update = buildTextUpdate("Сделал медитацию, всё отлично!", CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("сохранён");
        verify(homeworkRepository).save(homework);
        assertThat(homework.getStatus()).isEqualTo(Homework.Status.COMPLETED);
        assertThat(homework.getClientResponse()).isEqualTo("Сделал медитацию, всё отлично!");
        assertThat(homework.getRespondedAt()).isNotNull();
    }

    @Test
    @DisplayName("текстовое сообщение от клиента без активных заданий → сообщение об отсутствии")
    void textMessage_noActiveHomework_returnsNoTasksMessage() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).firstName("Иван").build();

        when(clientRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(client));
        when(homeworkRepository.findFirstByClientIdAndStatusOrderByCreatedAtDesc(clientId, Homework.Status.ASSIGNED))
                .thenReturn(Optional.empty());

        Update update = buildTextUpdate("Привет", CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("нет активных заданий");
        verify(homeworkRepository, never()).save(any());
    }

    @Test
    @DisplayName("сообщение от незнакомого chat_id → игнорируем (пустой Optional)")
    void textMessage_unknownChatId_returnsEmpty() {
        when(clientRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.empty());

        Update update = buildTextUpdate("Привет", CHAT_ID);
        Optional<String> reply = handler.handle(update);

        assertThat(reply).isEmpty();
        verifyNoInteractions(homeworkRepository);
    }

    @Test
    @DisplayName("update без сообщения → пустой Optional")
    void updateWithoutMessage_returnsEmpty() {
        Update update = new Update();
        Optional<String> reply = handler.handle(update);
        assertThat(reply).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Update buildTextUpdate(String text, Long chatId) {
        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setChat(chat);
        message.setText(text);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
