package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления библиотекой материалов практика.
 * Предоставляет операции создания, редактирования и архивирования
 * учебных и вспомогательных материалов.
 */
public interface MaterialService {

    /**
     * Возвращает список материалов практика.
     *
     * @param practitionerId  идентификатор практика
     * @param includeArchived {@code true} — включать архивные материалы в результат
     * @return список кратких данных о материалах
     */
    List<MaterialListItemResponse> getMaterials(UUID practitionerId, boolean includeArchived);

    /**
     * Создаёт новый материал в библиотеке практика.
     *
     * @param request        данные нового материала
     * @param practitionerId идентификатор практика
     * @return созданный материал
     */
    MaterialResponse createMaterial(MaterialRequest request, UUID practitionerId);

    /**
     * Обновляет существующий материал.
     *
     * @param id             идентификатор материала
     * @param request        новые данные материала
     * @param practitionerId идентификатор практика
     * @return обновлённый материал
     */
    MaterialResponse updateMaterial(UUID id, MaterialRequest request, UUID practitionerId);

    /**
     * Переводит материал в архив.
     *
     * @param id             идентификатор материала
     * @param practitionerId идентификатор практика
     */
    void archiveMaterial(UUID id, UUID practitionerId);

    /**
     * Восстанавливает материал из архива.
     *
     * @param id             идентификатор материала
     * @param practitionerId идентификатор практика
     */
    void unarchiveMaterial(UUID id, UUID practitionerId);
}
