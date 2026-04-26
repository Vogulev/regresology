package com.vogulev.regreso.config;

import com.vogulev.regreso.bot.RegresoBot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@DisplayName("TelegramBotConfig")
class TelegramBotConfigTest {

    @Test
    @DisplayName("регистрация бота не блокирует старт приложения")
    void registerTelegramBot_returnsImmediatelyWhenTelegramApiBlocks() throws Exception {
        TelegramBotsApi api = mock(TelegramBotsApi.class);
        RegresoBot bot = mock(RegresoBot.class);
        CountDownLatch registrationStarted = new CountDownLatch(1);

        doAnswer(invocation -> {
            registrationStarted.countDown();
            Thread.sleep(2_000);
            return null;
        }).when(api).registerBot(any(RegresoBot.class));

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(TelegramBotsApi.class, () -> api);
            context.registerBean(RegresoBot.class, () -> bot);
            context.refresh();

            TelegramBotConfig config = new TelegramBotConfig();
            ReflectionTestUtils.setField(config, "username", "RegresoBot");
            ApplicationReadyEvent event = new ApplicationReadyEvent(
                    new SpringApplication(Object.class),
                    new String[0],
                    context,
                    Duration.ZERO
            );

            long startedAt = System.nanoTime();
            config.registerTelegramBot(event);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertThat(registrationStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(elapsedMs).isLessThan(500);
        }
    }
}
