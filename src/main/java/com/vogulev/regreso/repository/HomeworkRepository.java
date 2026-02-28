package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с домашними заданиями клиентов.
 */
public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    /**
     * Возвращает количество заданий клиента с указанным статусом.
     * Используется, например, для подсчёта активных заданий в карточке клиента.
     *
     * @param clientId идентификатор клиента
     * @param status   статус задания
     * @return количество заданий с данным статусом
     */
    long countByClientIdAndStatus(UUID clientId, Homework.Status status);

    /**
     * Возвращает самое последнее задание клиента независимо от статуса.
     *
     * @param clientId идентификатор клиента
     * @return последнее задание клиента, если существует
     */
    Optional<Homework> findFirstByClientIdOrderByCreatedAtDesc(UUID clientId);

    /**
     * Возвращает самое последнее задание клиента с указанным статусом.
     *
     * @param clientId идентификатор клиента
     * @param status   статус задания
     * @return последнее задание с данным статусом, если существует
     */
    Optional<Homework> findFirstByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, Homework.Status status);

    /**
     * Возвращает все задания клиента с проверкой принадлежности практику,
     * отсортированные по дате создания (новые первыми).
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return список заданий клиента данного практика
     */
    List<Homework> findByClientIdAndPractitionerIdOrderByCreatedAtDesc(UUID clientId, UUID practitionerId);

    /**
     * Возвращает задание по его идентификатору с проверкой принадлежности практику.
     *
     * @param id             идентификатор задания
     * @param practitionerId идентификатор практика
     * @return задание, если найдено и принадлежит данному практику
     */
    Optional<Homework> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
