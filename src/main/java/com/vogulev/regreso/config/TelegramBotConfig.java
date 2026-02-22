package com.vogulev.regreso.config;

import com.vogulev.regreso.bot.BotUpdateHandler;
import com.vogulev.regreso.bot.RegresoBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Конфигурация Telegram бота.
 * Бины создаются только при telegram.bot.enabled=true.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotConfig {

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.username:RegresoBot}")
    private String username;

    @Bean
    public RegresoBot regresoBot(BotUpdateHandler handler) {
        return new RegresoBot(token, username, handler);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(RegresoBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        log.info("Telegram бот @{} запущен", username);
        return api;
    }
}
