package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MapStruct-маппер для преобразования сущности {@link Client} в DTO-ответы.
 * Статистические поля (сессии, задания) заполняются отдельно на уровне сервиса.
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

    /**
     * Преобразует сущность клиента в полный DTO-ответ для детальной карточки клиента.
     * Поля статистики (totalSessions, completedSessions, lastSessionAt, activeHomeworkCount)
     * не вычисляются на этом этапе и должны быть дозаполнены в сервисе.
     *
     * @param client сущность клиента
     * @return полный DTO-ответ клиента
     */
    @Mapping(target = "telegramConnected", expression = "java(client.getTelegramChatId() != null)")
    @Mapping(target = "totalSessions", constant = "0")
    @Mapping(target = "completedSessions", constant = "0")
    @Mapping(target = "lastSessionAt", ignore = true)
    @Mapping(target = "activeHomeworkCount", constant = "0")
    @Mapping(target = "presentingIssues", source = "presentingIssues", qualifiedByName = "arrayToList")
    ClientResponse toResponse(Client client);

    /**
     * Преобразует сущность клиента в краткий DTO-ответ для отображения в списке клиентов.
     * Поля статистики (totalSessions, lastSessionAt, activeHomeworkCount)
     * не вычисляются на этом этапе и должны быть дозаполнены в сервисе.
     *
     * @param client сущность клиента
     * @return краткий DTO-ответ клиента для списка
     */
    @Mapping(target = "telegramConnected", expression = "java(client.getTelegramChatId() != null)")
    @Mapping(target = "totalSessions", constant = "0")
    @Mapping(target = "lastSessionAt", ignore = true)
    @Mapping(target = "activeHomeworkCount", constant = "0")
    @Mapping(target = "presentingIssues", source = "presentingIssues", qualifiedByName = "arrayToList")
    ClientListItemResponse toListItemResponse(Client client);

    @Named("arrayToList")
    default List<String> arrayToList(String[] array) {
        return array != null ? Arrays.asList(array) : Collections.emptyList();
    }
}
