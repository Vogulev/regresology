package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.SessionListItemResponse;
import com.vogulev.regreso.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "status",
            expression = "java(session.getStatus() != null ? session.getStatus().name() : null)")
    @Mapping(target = "regressionTarget",
            expression = "java(session.getRegressionTarget() != null ? session.getRegressionTarget().name() : null)")
    SessionListItemResponse toListItemResponse(Session session);
}
