package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.SessionListItemResponse;
import com.vogulev.regreso.dto.response.SessionResponse;
import com.vogulev.regreso.dto.response.SessionSummaryResponse;
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

    @Mapping(target = "regressionTarget",
            expression = "java(session.getRegressionTarget() != null ? session.getRegressionTarget().name() : null)")
    SessionSummaryResponse toSummaryResponse(Session session);
}
