package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.entity.PractitionerMaterial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    @Mapping(target = "isArchived", expression = "java(Boolean.TRUE.equals(entity.getIsArchived()))")
    MaterialResponse toResponse(PractitionerMaterial entity);

    @Mapping(target = "isArchived", expression = "java(Boolean.TRUE.equals(entity.getIsArchived()))")
    MaterialListItemResponse toListItemResponse(PractitionerMaterial entity);
}
