package com.vogulev.regreso.bot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram Long Polling Bot.
 * Бизнес-логика делегирована в {@link BotUpdateHandler}.
 * Бин создаётся только при telegram.bot.enabled=true (через TelegramBotConfig).
 */
@Slf4j
public class RegresoBot extends TelegramLongPollingBot {

    private final String username;
    private final BotUpdateHandler handler;

    public RegresoBot(String token, String username, BotUpdateHandler handler) {
        super(token);
        this.username = username;
        this.handler = handler;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            handler.handle(update).ifPresent(replyText -> {
                if (update.hasMessage()) {
                    Long chatId = update.getMessage().getChatId();
                    sendReply(chatId, replyText);
                }
            });
        } catch (Exception e) {
            log.error("Ошибка обработки Telegram update: {}", e.getMessage(), e);
        }
    }

    private void sendReply(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа в Telegram chatId={}: {}", chatId, e.getMessage());
        }
    }
}
