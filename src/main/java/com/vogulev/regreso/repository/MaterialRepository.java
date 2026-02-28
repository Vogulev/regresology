package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.PractitionerMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с материалами библиотеки практика.
 */
public interface MaterialRepository extends JpaRepository<PractitionerMaterial, UUID> {

    /**
     * Возвращает все активные (не архивированные) материалы практика,
     * отсортированные по дате создания (новые первыми).
     *
     * @param practitionerId идентификатор практика
     * @return список активных материалов
     */
    List<PractitionerMaterial> findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID practitionerId);

    /**
     * Возвращает все материалы практика, включая архивированные,
     * отсортированные по дате создания (новые первыми).
     *
     * @param practitionerId идентификатор практика
     * @return полный список материалов, включая архив
     */
    List<PractitionerMaterial> findByPractitionerIdOrderByCreatedAtDesc(UUID practitionerId);

    /**
     * Возвращает материал по его идентификатору с проверкой принадлежности практику.
     *
     * @param id             идентификатор материала
     * @param practitionerId идентификатор практика
     * @return материал, если найден и принадлежит данному практику
     */
    Optional<PractitionerMaterial> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
