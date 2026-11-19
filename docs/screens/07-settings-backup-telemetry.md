# Настройки, резервное копирование и телеметрия (`SettingsSheet`, DataStore, Backup, Crashlytics)

**Исходные файлы:**
- `app/src/main/java/com/example/ui/screens/SettingsSheet.kt` (`SettingsDialog`)
- `app/src/main/java/com/example/data/UserPreferencesRepository.kt`
- `core/src/main/java/com/example/core/BackupCodec.kt`
- `app/src/main/java/com/example/telemetry/CrashReporter.kt`
- `app/src/main/java/com/example/IronLogApplication.kt`

---

## 1. Продуктовое описание

### Назначение
Модальное окно настроек (`SettingsDialog`) открывается с главного экрана («Тренировки») и даёт пользователю полный контроль над единицами измерения, интерфейсом, поведением таймера отдыха, ручной синхронизацией с часами, резервным копированием и полным сбросом локальных данных.

### Основные разделы настроек
1. **Единицы измерения, язык и тема:**
   - Переключение единиц веса: килограммы (`KG`) или фунты (`LBS`).
   - Переключение языка интерфейса: русский (`RU`) или английский (`EN`).
   - Выбор темы оформления: Тёмная (`DARK`), Светлая (`LIGHT`) или Системная (`SYSTEM`).
2. **Таймер отдыха:**
   - Независимые тумблеры для звукового сигнала, вибрации по окончании отдыха и автозапуска таймера отдыха сразу после завершения подхода.
3. **Локальная синхронизация с Wear OS (`wear_sync_button`):**
   - Карточка отображает статус подключения часов (количество найденных узлов, время последней синхронизации) и кнопку ручной синхронизации «Синхронизировать сейчас».
4. **Локальное резервное копирование и экспорт:**
   - **Экспорт JSON:** сохраняет полную резервную копию тренировок, программ, упражнений и удалений в выбранный пользователем файл.
   - **Экспорт CSV:** выгружает детализированную таблицу всех подходов для открытия в Excel / Google Таблицах.
   - **Импорт JSON:** восстанавливает или объединяет данные из ранее сохранённой резервной копии с проверкой целостности файла.
5. **Очистка всех данных:**
   - Кнопка сброса с диалогом подтверждения удаляет пользовательские записи и восстанавливает стартовый каталог из 49 упражнений и 3 базовых программ.

---

## 2. Техническое описание

### Хранение настроек (`UserPreferencesRepository`)
- Использует **Jetpack DataStore Preferences** (`ironlog_prefs`).
- Помимо пользовательских настроек (`WeightUnit`, `AppLanguage`, `ThemeMode`, `OneRmFormula`, флаги таймера), хранит:
  - уникальный `device_id` формата `phone-<UUID>` для разрешения конфликтов при импорте и синхронизации;
  - `last_sync_ms` (метку времени последнего обмена с Wear OS);
  - служебные метаданные активного таймера отдыха.

### Безопасный доступ к файлам (SAF)
- `SettingsSheet.kt` использует системные контракты Android **Storage Access Framework**:
  - `ActivityResultContracts.CreateDocument` с MIME-типами `application/json` и `text/csv` для экспорта;
  - `ActivityResultContracts.OpenDocument` для импорта JSON.
- Приложение **не запрашивает** широкие разрешения на доступ к хранилищу (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`) в соответствии с политиками Google Play.

### Формат резервной копии (`BackupCodec`)
- **JSON (`SyncEnvelope`):**
  - При экспорте вычисляется контрольная сумма `SHA-256` по каноническому компактному JSON (с пустым полем `sha256Checksum`), после чего хеш записывается в конверт и документ форматируется с отступами.
  - При импорте проверяется `schemaVersion == 1` и совпадение `SHA-256`. При успехе вызывается `WorkoutRepository.mergeIncomingEnvelope`: применяется правило «побеждает самый свежий `updatedAt`», игнорируются собственные записи с тем же `sourceDeviceId`, и учитываются tombstone-удаления (`deletions`).
- **CSV (16 колонок):**
  - Заголовок: `session_id,date,workout_name,duration_min,source_device,avg_hr,exercise_name,muscle_group,set_index,set_type,weight_kg,reps,duration_sec,distance_m,rpe,completed`.

### Телеметрия и отчёты о сбоях (`CrashReporter`)
- Инициализируется в `IronLogApplication` поверх **Firebase Crashlytics** (`firebase-crashlytics`, все вызовы обёрнуты в `runCatching`).
- Автоматически отключает отправку отчётов в среде **Robolectric** (`Build.FINGERPRINT == "robolectric"`), чтобы локальные JVM-тесты не зависели от нативного окружения.
- Записывает пользовательские ключи контекста (`build_type`, `app_version`, `version_code`, `weight_unit`, `app_language`, `theme_mode`, `rm_formula`), навигационные и тренировочные «хлебные крошки» (`breadcrumbs`), а также нефатальные исключения (`db_seed`, `wear_nodes`, `wear_sync`, `wear_decode`, `backup_import`).
- Использует только обычные сетевые разрешения `INTERNET` и `ACCESS_NETWORK_STATE`; при работе в офлайн-режиме Crashlytics кеширует отчёты локально до появления сети.
