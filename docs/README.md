# Документация IronLog

Этот раздел содержит подробную **продуктовую** (пользовательские сценарии, логика работы, ограничения и UX) и **техническую** (архитектура, состояние, БД, сервисы, алгоритмы) документацию по всему приложению **IronLog**.

---

## Навигация по документации

### Экраны мобильного приложения (`:app`)
1. [Тренировка и программы (`HomeAndRoutinesScreen`)](screens/01-home-and-routines.md)
2. [Активная тренировка и сводка (`ActiveWorkoutScreen`)](screens/02-active-workout.md)
3. [Каталог упражнений (`CatalogScreen`)](screens/03-catalog.md)
4. [История и календарь (`HistoryScreen`)](screens/04-history.md)
5. [Аналитика и тепловая карта мышц (`AnalyticsScreen`)](screens/05-analytics.md)
6. [Силовые калькуляторы (`CalculatorsScreen`)](screens/06-calculators.md)
7. [Настройки, резервное копирование и Crashlytics (`SettingsDialog`, `CrashReporter`)](screens/07-settings-backup-telemetry.md)

### Компаньон для часов (`:wear`)
8. [Wear OS приложение, датчики и локальная синхронизация](screens/08-wear-os.md)

---

## Обзор архитектуры

### 1. Модульное разделение
- **`:core`** (чистый Android Library-модуль без UI):
  - `DomainEnums.kt`, `SyncModels.kt` — доменные модели (`ExerciseDto`, `RoutineDto`, `WorkoutSessionDto`, `SyncEnvelope`, `WearSyncAck`).
  - `StrengthCalculators` — чистые функции расчёта подбора блинов, разминки, 1ПМ (4 формулы), объёма подходов/сессий, личных рекордов (`PersonalRecord`) и нагрузки по мышечным группам.
  - `BackupCodec` — JSON-кодирование/верификация с `SHA-256`, компактная сериализация для Wear Data Layer, генерация CSV-истории.
  - `ExerciseCatalogSeed` — стартовый набор из **49** упражнений, **2** папок (`Сила / Strength`, `Гипертрофия / PPL`) и **3** программ (`Push A`, `Pull A`, `Legs A`).
  - `StringsProvider` — полный словарь локализации `RU` / `EN`.
- **`:app`** (Android-приложение для смартфонов и планшетов):
  - `IronLogApplication` — точка инициализации Room (`IronLogDatabase`), DataStore (`UserPreferencesRepository`), `WorkoutRepository`, `WearSyncManager` и `CrashReporter`.
  - `IronLogViewModel` — единый `AndroidViewModel`, объединяющий потоки Room/DataStore/FGS в `StateFlow` для Compose UI.
  - `IronLogRoot` — корневой граф Navigation Compose с адаптивным переключением `NavigationBar` (`< 600.dp`) / `NavigationRail` (`>= 600.dp`) и скрытием панели навигации во время активной тренировки.
  - `WorkoutTimerService` — `ForegroundService` типа `specialUse` для точного отсчёта времени тренировки и таймера отдыха в фоне.
- **`:wear`** (самостоятельное приложение для Wear OS):
  - Независимая локальная БД `WearDatabase` (`ironlog_wear_offline.db`), `WearViewModel`, `HeartRateSensorMonitor` и `WearDataListenerService`.

### 2. Карта маршрутов Navigation Compose (`:app`)

| Маршрут (`@Serializable`) | Экран | Расположение в навигации |
| --- | --- | --- |
| `RouteTrain` | `HomeAndRoutinesScreen` | Вкладка 1 («Тренировка»), стартовый экран графа |
| `RouteCatalog` | `CatalogScreen` | Вкладка 2 («Упражнения») |
| `RouteHistory` | `HistoryScreen` | Вкладка 3 («История») |
| `RouteAnalytics` | `AnalyticsScreen` | Вкладка 4 («Аналитика») |
| `RouteCalculators` | `CalculatorsScreen` | Вкладка 5 («Калькуляторы») |
| `RouteActiveWorkout` | `ActiveWorkoutScreen` | Полноэкранный маршрут без нижней/боковой панели навигации |

Настройки (`SettingsDialog`) и итоги завершённой тренировки (`WorkoutSummaryDialog`) открываются как модальные диалоги поверх корневого `BoxWithConstraints` в `IronLogRoot`.

### 3. Локальная база данных Room (`ironlog_offline.db`)
База данных состоит из 9 сущностей с каскадным удалением (`ForeignKey.CASCADE`):
- `exercises` — справочник упражнений (базовые + пользовательские).
- `routine_folders` — папки программ с поддержкой soft-delete (`isDeleted`).
- `routines` и `routine_exercises` — шаблоны тренировок и их упражнения.
- `workout_sessions`, `workout_exercises`, `workout_sets` — активные и завершённые сессии, упражнения и подходы.
- `heart_rate_samples` — временные ряды пульса, приходящие с Wear OS.
- `sync_operations` — журнал изменений и удалений (tombstones) для синхронизации с часами.

### 4. Ключевые принципы дизайна и надёжности
- **Единица хранения веса:** все веса в БД и расчётах всегда хранятся в **килограммах (`weightKg`)**, а отображение и ввод конвертируются на лету через `WeightUnit.toDisplay()` / `WeightUnit.toKg()`.
- **Офлайн по умолчанию:** ни одна пользовательская функция не требует сети; Crashlytics буферизует отчёты локально.
- **Политики Google Play:** экспорт/импорт файлов реализован через `ActivityResultContracts.CreateDocument` / `OpenDocument` (Storage Access Framework) без запроса широких разрешений к хранилищу; доступ к Wear OS защищён проверкой `GoogleApiAvailability`.
