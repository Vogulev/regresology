package com.vogulev.regreso.mapper;

import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "telegramConnected", expression = "java(client.getTelegramChatId() != null)")
    @Mapping(target = "totalSessions", constant = "0")
    @Mapping(target = "completedSessions", constant = "0")
    @Mapping(target = "lastSessionAt", ignore = true)
    @Mapping(target = "activeHomeworkCount", constant = "0")
    ClientResponse toResponse(Client client);

    @Mapping(target = "telegramConnected", expression = "java(client.getTelegramChatId() != null)")
    @Mapping(target = "totalSessions", constant = "0")
    @Mapping(target = "lastSessionAt", ignore = true)
    @Mapping(target = "activeHomeworkCount", constant = "0")
    ClientListItemResponse toListItemResponse(Client client);
}
