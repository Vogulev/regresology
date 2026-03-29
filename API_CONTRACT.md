# Regresology — API Contract (Request/Response DTO)

// =============================================
// AUTH — request/response DTO
// =============================================

// POST /api/auth/register
public record RegisterRequest(
    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 8, max = 72)
    String password,

    @NotBlank @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName,

    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    String phone
) {}

// POST /api/auth/login
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

// POST /api/auth/refresh
public record RefreshRequest(
    @NotBlank String refreshToken
) {}

// Response для login и refresh
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,   // unix timestamp
    PractitionerShortResponse practitioner
) {}

// Краткий профиль — вернуть сразу после логина
public record PractitionerShortResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String plan,
    OffsetDateTime planExpiresAt
) {}
// =============================================
// CLIENTS — request/response DTO
// =============================================

// POST /api/clients
// PUT  /api/clients/{id}
public record ClientRequest(
    @NotBlank @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName,

    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    String phone,

    @Email
    String email,

    LocalDate birthDate,

    @Size(max = 100)
    String telegramUsername,

    // Первичный запрос — почему пришёл
    String initialRequest,

    // ["страхи", "отношения", "здоровье", "самореализация", "другое"]
    List<String> presentingIssues,

    // Медицинский скрининг
    Boolean hasContraindications,
    String contraindicationsNotes,
    Boolean intakeFormCompleted,

    String generalNotes
) {}

// GET /api/clients/{id} — полная карточка
public record ClientResponse(
    UUID id,
    String firstName,
    String lastName,
    String fullName,        // firstName + lastName
    String phone,
    String email,
    LocalDate birthDate,
    String telegramUsername,
    boolean telegramConnected,   // telegramChatId != null

    String initialRequest,
    List<String> presentingIssues,

    // Противопоказания — показывать как предупреждение на UI
    boolean hasContraindications,
    String contraindicationsNotes,
    boolean intakeFormCompleted,

    String overallProgress,
    String generalNotes,
    boolean isArchived,

    // Статистика (вычисляется на бэке)
    int totalSessions,
    int completedSessions,
    OffsetDateTime lastSessionAt,
    int activeHomeworkCount,

    OffsetDateTime createdAt
) {}

// GET /api/clients — список (лёгкая версия без статистики)
public record ClientListItemResponse(
    UUID id,
    String fullName,
    String phone,
    LocalDate birthDate,
    boolean hasContraindications,  // для иконки предупреждения
    boolean telegramConnected,
    int totalSessions,
    OffsetDateTime lastSessionAt,
    List<String> presentingIssues, // показываем первые 3 тега
    int activeHomeworkCount,
    boolean isArchived
) {}

// PATCH /api/clients/{id}/archive — архивировать / разархивировать
public record ArchiveRequest(boolean archive) {}

// PATCH /api/clients/{id}/progress — обновить общий прогресс
public record UpdateProgressRequest(
    @NotBlank String overallProgress
) {}
// =============================================
// SESSIONS — request/response DTO
// =============================================

// POST /api/sessions — создать/запланировать
public record CreateSessionRequest(
    @NotNull UUID clientId,
    @NotNull OffsetDateTime scheduledAt,
    Integer durationMin,       // default: из настроек практика (120)
    String preSessionRequest,  // запрос клиента можно задать заранее
    BigDecimal price
) {}

// PUT /api/sessions/{id} — обновить протокол (частично, любые поля)
public record UpdateSessionRequest(
    OffsetDateTime scheduledAt,
    Integer durationMin,

    // [1] Перед
    String preSessionRequest,
    String preSessionState,
    Short preSessionScore,

    // [2] Транс
    String inductionMethod,
    String tranceDepth,        // LIGHT | MEDIUM | DEEP
    String inductionNotes,

    // [3] Регрессия
    String regressionTarget,   // PAST_LIFE | CHILDHOOD | PRENATAL | BETWEEN_LIVES | OTHER
    String regressionPeriod,
    String regressionSetting,

    // [4] Ключевые сцены
    String keyScenes,
    List<String> keyEmotions,
    String keyInsights,
    String symbolicImages,

    // [5] Проработка
    String blocksReleased,
    Boolean healingOccurred,
    String healingNotes,

    // [6] Выход
    String postSessionState,
    Short postSessionScore,
    String integrationNotes,

    // [7] Итог (приватные заметки практика)
    String practitionerNotes,
    String nextSessionPlan,

    // Финансы
    BigDecimal price,
    Boolean isPaid
) {}

// POST /api/sessions/{id}/complete — завершить сессию
// (статус → COMPLETED, создать напоминания для следующей сессии)
// body: UpdateSessionRequest (финальные поля если не заполнены)

// POST /api/sessions/{id}/cancel
public record CancelSessionRequest(
    String reason  // опционально
) {}

// GET /api/sessions/{id} — полный протокол
public record SessionResponse(
    UUID id,
    int sessionNumber,
    String status,

    // Клиент (краткая инфо)
    UUID clientId,
    String clientFullName,
    boolean clientHasContraindications,  // показать предупреждение!

    OffsetDateTime scheduledAt,
    int durationMin,

    // Все поля протокола
    String preSessionRequest,
    String preSessionState,
    Short preSessionScore,

    String inductionMethod,
    String tranceDepth,
    String inductionNotes,

    String regressionTarget,
    String regressionPeriod,
    String regressionSetting,

    String keyScenes,
    List<String> keyEmotions,
    String keyInsights,
    String symbolicImages,

    String blocksReleased,
    Boolean healingOccurred,
    String healingNotes,

    String postSessionState,
    Short postSessionScore,
    String integrationNotes,

    String practitionerNotes,   // ТОЛЬКО для практика, никогда не передавать клиенту
    String nextSessionPlan,

    BigDecimal price,
    boolean isPaid,

    List<SessionMediaResponse> media,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

public record SessionMediaResponse(
    UUID id,
    String mediaType,   // PHOTO
    String fileUrl,
    String fileName,
    String mimeType,
    Integer fileSizeBytes,
    Integer durationSec,
    String caption,
    OffsetDateTime createdAt
) {}

// POST /api/sessions/{id}/photos — загрузить фото рукописного протокола
// multipart/form-data:
// - file: image/*
// - caption: optional
// Использование:
// - фото страниц тетради или журнала практика
// - фото рукописных заметок, схем, символов, рисунков клиента
// После загрузки файл сразу должен появляться в SessionResponse.media на экране сессии.
// Фронтенд может вызывать endpoint последовательно для множественной загрузки нескольких файлов.
//
// DELETE /api/sessions/{id}/photos/{mediaId} — удалить фото протокола
// Ответ: 204 No Content

// GET /api/sessions/{id} — лёгкая версия для списков
public record SessionListItemResponse(
    UUID id,
    int sessionNumber,
    String status,
    OffsetDateTime scheduledAt,
    int durationMin,
    String preSessionRequest,   // краткий запрос
    String regressionTarget,
    String keyInsights,         // превью инсайтов
    BigDecimal price,
    boolean isPaid
) {}

// GET /api/sessions/{id}/prepare — экран "Подготовка к сессии"
// Всё что нужно знать за 5 минут до встречи — только чтение
public record SessionPrepResponse(
    UUID sessionId,
    OffsetDateTime scheduledAt,
    int sessionNumber,

    // Клиент
    UUID clientId,
    String clientFullName,
    LocalDate clientBirthDate,
    boolean hasContraindications,
    String contraindicationsNotes,   // красное предупреждение если есть!
    String initialRequest,
    List<String> presentingIssues,

    // Последние 3 сессии
    List<SessionSummary> recentSessions,

    // Активные темы
    List<ClientThemeShortResponse> activeThemes,

    // Задание из прошлой сессии
    HomeworkShortResponse lastHomework,

    // План который практик ставил на эту сессию
    String nextSessionPlan  // из предыдущей сессии
) {}

// Краткое саммари сессии для экрана подготовки
public record SessionSummary(
    UUID id,
    int sessionNumber,
    OffsetDateTime scheduledAt,
    String regressionTarget,
    String regressionPeriod,
    String keyInsights,
    String blocksReleased
) {}

// Медиафайл
public record SessionMediaResponse(
    UUID id,
    String mediaType,
    String fileName,
    String url,           // presigned URL из MinIO
    Long fileSizeBytes,
    Integer durationSec,  // для аудио
    String caption,
    OffsetDateTime createdAt
) {}
// =============================================
// SCHEDULE — расписание
// =============================================

// GET /api/schedule/today
// GET /api/schedule/week
public record ScheduleResponse(
    List<ScheduleSessionItem> sessions,
    int totalCount,
    int completedCount,
    int upcomingCount
) {}

public record ScheduleSessionItem(
    UUID id,
    UUID clientId,
    String clientFullName,
    boolean clientHasContraindications,
    int sessionNumber,          // "3-я сессия"
    OffsetDateTime scheduledAt,
    int durationMin,
    String status,
    String preSessionRequest,   // превью запроса
    boolean isPaid,
    boolean telegramReminderSent
) {}


// =============================================
// MATERIALS — библиотека материалов практика
// =============================================

// GET /api/materials?includeArchived=false
public record MaterialListItemResponse(
    UUID id,
    String title,
    String materialType,
    String fileUrl,
    String fileName,
    String mimeType,
    Long fileSizeBytes,
    boolean isArchived,
    OffsetDateTime createdAt
) {}

// POST /api/materials
// PUT /api/materials/{id}
// Поддерживаются 2 формата:
// 1) application/json
public record MaterialRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 1024) String description,
    @NotBlank @Size(max = 50) String materialType,
    String content
) {}

// 2) multipart/form-data
// - title: string (required)
// - description: string (optional)
// - materialType: string (required)
// - content: string (optional)
// - file: binary (optional)
//
// Если file передан, бэкенд сохраняет его и возвращает метаданные файла.

public record MaterialResponse(
    UUID id,
    String title,
    String description,
    String materialType,
    String content,
    String fileUrl,
    String fileName,
    String mimeType,
    Long fileSizeBytes,
    boolean isArchived,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}


// =============================================
// HOMEWORK — домашние задания
// =============================================

// POST /api/clients/{clientId}/homework
// POST /api/sessions/{sessionId}/homework  (привязать к конкретной сессии)
public record CreateHomeworkRequest(
    @NotBlank String title,
    @NotBlank String description,
    String homeworkType,    // JOURNALING | MEDITATION | BODYWORK | OBSERVATION | OTHER
    LocalDate dueDate
) {}

public record HomeworkResponse(
    UUID id,
    UUID sessionId,
    UUID clientId,
    String clientFullName,
    String title,
    String description,
    String homeworkType,
    LocalDate dueDate,
    String status,
    String clientResponse,
    OffsetDateTime respondedAt,
    OffsetDateTime createdAt
) {}

// Краткая версия для других экранов
public record HomeworkShortResponse(
    UUID id,
    String title,
    String status,
    LocalDate dueDate,
    String clientResponse  // null если не ответил
) {}

// PATCH /api/homework/{id}/status
public record UpdateHomeworkStatusRequest(
    @NotNull String status  // COMPLETED | SKIPPED
) {}


// =============================================
// CLIENT THEMES — сквозные темы
// =============================================

// POST /api/clients/{clientId}/themes
public record CreateThemeRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    OffsetDateTime firstSeenAt   // если не указано — now()
) {}

// POST /api/themes/{themeId}/sessions — привязать сессию к теме
public record LinkSessionToThemeRequest(
    @NotNull UUID sessionId,
    String notes
) {}

public record ClientThemeResponse(
    UUID id,
    String title,
    String description,
    boolean isResolved,
    OffsetDateTime resolvedAt,
    OffsetDateTime firstSeenAt,
    int sessionsCount,
    List<SessionSummaryShort> sessions  // в каких сессиях прорабатывалась
) {}

public record ClientThemeShortResponse(
    UUID id,
    String title,
    boolean isResolved,
    int sessionsCount
) {}

public record SessionSummaryShort(
    UUID id,
    int sessionNumber,
    OffsetDateTime scheduledAt
) {}

// PATCH /api/themes/{id}/resolve — пометить тему как проработанную
// (без body, просто action)


// =============================================
// STATS — статистика
// =============================================

// GET /api/stats/summary — дашборд за текущий месяц
public record StatsSummaryResponse(
    // Сессии
    int sessionsThisMonth,
    int sessionsLastMonth,
    int sessionsTotal,

    // Клиенты
    int totalClients,
    int newClientsThisMonth,
    int activeClients,        // были сессии за последние 90 дней

    // Финансы
    BigDecimal revenueThisMonth,
    BigDecimal revenueLastMonth,
    BigDecimal revenueTotal,

    // Задания
    int homeworkCompletionRate,   // % выполненных заданий

    // Требуют внимания
    int unpaidSessionsCount,
    int clientsWithoutSessionOver60Days  // для реактивации
) {}

// GET /api/stats/sessions?from=&to= — сессии по месяцам (для графика)
public record SessionsChartResponse(
    List<MonthlyStats> data
) {}

public record MonthlyStats(
    String month,    // "2025-01"
    int count,
    BigDecimal revenue
) {}

// GET /api/stats/topics — топ запросов клиентов
public record TopicsStatsResponse(
    List<TopicCount> presentingIssues,
    List<TopicCount> regressionTargets,
    List<TopicCount> keyEmotions
) {}

public record TopicCount(
    String topic,
    int count,
    int percentage
) {}
// =============================================
// SETTINGS — настройки
// =============================================

// GET /api/settings/profile
// PUT /api/settings/profile
public record ProfileSettingsRequest(
    @NotBlank String firstName,
    String lastName,
    @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
    String bio,
    String timezone,
    Integer defaultSessionDurationMin
) {}

public record ProfileSettingsResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    String bio,
    String timezone,
    Integer defaultSessionDurationMin,
    Long telegramChatId,
    boolean telegramConnected,
    String plan,
    OffsetDateTime planExpiresAt
) {}

// GET /api/settings/booking
// PUT /api/settings/booking
public record BookingSettingsRequest(
    boolean isEnabled,
    @Pattern(regexp = "^[a-z0-9-]{3,50}$") String slug,
    Integer defaultDurationMin,
    Integer bufferMin,
    Integer advanceDays,
    boolean requireIntakeForm,
    List<BookingServiceItem> services,
    String welcomeMessage,
    BookingAvailabilityMode availabilityMode,
    List<BookingDayAvailability> weeklyAvailability
) {}

public record BookingSettingsResponse(
    boolean isEnabled,
    String slug,
    String bookingUrl,         // полный URL: regresology.ru/book/{slug}
    Integer defaultDurationMin,
    Integer bufferMin,
    Integer advanceDays,
    boolean requireIntakeForm,
    List<BookingServiceItem> services,
    String welcomeMessage,
    BookingAvailabilityMode availabilityMode,
    List<BookingDayAvailability> weeklyAvailability
) {}

public record BookingServiceItem(
    @NotBlank String name,
    BigDecimal price,
    Integer durationMin
) {}

public enum BookingAvailabilityMode { DEFAULT, CUSTOM }

public record BookingDayAvailability(
    int dayOfWeek,
    boolean isWorkingDay,
    String startTime,
    String endTime,
    Integer slotIntervalMin,
    List<BookingBreakItem> breaks
) {}

public record BookingBreakItem(
    String startTime,
    String endTime
) {}

// Telegram — GET /api/settings/telegram/link
// Генерирует одноразовую ссылку для подключения бота практиком
public record TelegramLinkResponse(
    String botUrl,       // t.me/RegresologyBot?start=PRACTITIONER_{id}
    boolean connected,
    Long telegramChatId
) {}


// =============================================
// PUBLIC BOOKING — публичная запись (без auth)
// =============================================

// GET /api/public/booking/{slug} — страница записи
public record PublicBookingPageResponse(
    String practitionerName,
    String practitionerBio,
    String welcomeMessage,
    List<BookingServiceItem> services,
    boolean requireIntakeForm,
    Integer advanceDays
) {}

// GET /api/public/booking/{slug}/slots?date=2025-03-01
public record AvailableSlotsResponse(
    List<AvailableSlot> slots
) {}

public record AvailableSlot(
    OffsetDateTime startsAt,
    OffsetDateTime endsAt
) {}

// POST /api/public/booking/{slug} — записаться
public record PublicBookingRequest(
    @NotBlank String firstName,
    String lastName,
    @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
    String email,
    @NotNull OffsetDateTime selectedSlot,
    @NotBlank String serviceName,
    String clientRequest,           // запрос / вопрос
    IntakeFormData intakeForm       // если required
) {}

public record IntakeFormData(
    LocalDate birthDate,
    boolean hasMentalHealthHistory,
    boolean takingMedications,
    String medicationsNotes,
    boolean hasEpilepsy,
    String otherHealthNotes
) {}

public record PublicBookingConfirmation(
    String message,         // "Вы записаны! Ждём вас 15 марта в 14:00"
    OffsetDateTime sessionAt,
    String practitionerName
) {}


// =============================================
// ERRORS — единый формат ошибок
// =============================================

// Всегда возвращать в таком формате
public record ErrorResponse(
    int status,
    String code,        // RESOURCE_NOT_FOUND | ACCESS_DENIED | VALIDATION_ERROR | ...
    String message,
    Map<String, String> fieldErrors,  // для валидационных ошибок (поле → сообщение)
    OffsetDateTime timestamp
) {}


# AI Саммари

// GET /api/sessions/{id}/summary — polling после завершения сессии
public record SessionSummaryStatusResponse(
    UUID sessionId,
    SummaryStatus status,   // PENDING | READY | FAILED
    String summary,         // null пока PENDING
    OffsetDateTime generatedAt
) {}

// POST /api/sessions/{id}/summary/generate — ручная регенерация
// POST /api/clients/{id}/summary/generate  — общее саммари клиента
public record GenerateSummaryResponse(
    String summary,
    OffsetDateTime generatedAt
) {}

// Добавить в SessionPrepResponse:
// String lastSessionAiSummary       — саммари прошлой сессии
// String clientAiOverallSummary     — общая динамика клиента

// Добавить в SessionResponse:
// String aiSummary
// OffsetDateTime aiSummaryGeneratedAt
// SummaryStatus aiSummaryStatus

// Добавить в ClientResponse:
// String aiOverallSummary
// OffsetDateTime aiOverallSummaryGeneratedAt
