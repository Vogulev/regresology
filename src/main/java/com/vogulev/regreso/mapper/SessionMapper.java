package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.SessionListItemResponse;
import com.vogulev.regreso.dto.response.SessionResponse;
import com.vogulev.regreso.dto.response.SessionSummaryResponse;
import com.vogulev.regreso.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct-маппер для преобразования сущности {@link Session} в различные DTO-ответы.
 * Enum-поля (статус, глубина транса, цель регрессии) преобразуются в строки через {@code .name()},
 * при этом безопасно обрабатывается значение null.
 */
@Mapper(componentModel = "spring")
public interface SessionMapper {

    /**
     * Преобразует сущность сессии в краткий DTO-ответ для отображения в списках и расписании.
     * Enum-поля status и regressionTarget конвертируются в строковые имена.
     *
     * @param session сущность сессии
     * @return краткий DTO-ответ сессии для списка
     */
    @Mapping(target = "status",
            expression = "java(session.getStatus() != null ? session.getStatus().name() : null)")
    @Mapping(target = "regressionTarget",
            expression = "java(session.getRegressionTarget() != null ? session.getRegressionTarget().name() : null)")
    SessionListItemResponse toListItemResponse(Session session);

    /**
     * Преобразует сущность сессии в полный DTO-ответ с данными протокола и информацией о клиенте.
     * Включает флаг противопоказаний клиента; все enum-поля конвертируются в строки.
     *
     * @param session сущность сессии (с загруженной связью client)
     * @return полный DTO-ответ сессии
     */
    @Mapping(target = "clientId",          source = "client.id")
    @Mapping(target = "clientFullName",    expression = "java(session.getClient().getFullName())")
    @Mapping(target = "clientHasContraindications",
            expression = "java(Boolean.TRUE.equals(session.getClient().getHasContraindications()))")
    @Mapping(target = "status",
            expression = "java(session.getStatus() != null ? session.getStatus().name() : null)")
    @Mapping(target = "tranceDepth",
            expression = "java(session.getTranceDepth() != null ? session.getTranceDepth().name() : null)")
    @Mapping(target = "regressionTarget",
            expression = "java(session.getRegressionTarget() != null ? session.getRegressionTarget().name() : null)")
    SessionResponse toResponse(Session session);

    /**
     * Преобразует сущность сессии в DTO-ответ с саммари для экрана подготовки к следующей сессии.
     * Содержит AI-сгенерированное резюме и ключевые параметры завершённой сессии.
     *
     * @param session сущность завершённой сессии
     * @return DTO-ответ саммари сессии
     */
    @Mapping(target = "regressionTarget",
            expression = "java(session.getRegressionTarget() != null ? session.getRegressionTarget().name() : null)")
    SessionSummaryResponse toSummaryResponse(Session session);
}
