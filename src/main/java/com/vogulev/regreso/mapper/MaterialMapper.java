package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.entity.PractitionerMaterial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct-маппер для преобразования сущности {@link PractitionerMaterial} в DTO-ответы.
 * Явно нормализует поле isArchived во избежание NPE при значении null.
 */
@Mapper(componentModel = "spring")
public interface MaterialMapper {

    /**
     * Преобразует сущность материала в полный DTO-ответ.
     * Поле {@code isArchived} нормализуется: null трактуется как {@code false}.
     *
     * @param entity сущность материала практика
     * @return полный DTO-ответ материала
     */
    @Mapping(target = "isArchived", expression = "java(Boolean.TRUE.equals(entity.getIsArchived()))")
    MaterialResponse toResponse(PractitionerMaterial entity);

    /**
     * Преобразует сущность материала в краткий DTO-ответ для отображения в списке.
     * Поле {@code isArchived} нормализуется: null трактуется как {@code false}.
     *
     * @param entity сущность материала практика
     * @return краткий DTO-ответ материала для списка
     */
    @Mapping(target = "isArchived", expression = "java(Boolean.TRUE.equals(entity.getIsArchived()))")
    MaterialListItemResponse toListItemResponse(PractitionerMaterial entity);
}
