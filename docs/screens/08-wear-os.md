# Приложение для Wear OS (`:wear` — WearHomeScreen, WearActiveWorkoutPager, WearSummaryScreen)

**Исходные файлы:**
- `wear/src/main/java/com/example/wear/WearMainActivity.kt`
- `wear/src/main/java/com/example/wear/WearViewModel.kt`
- `wear/src/main/java/com/example/wear/data/WearDatabase.kt` (`ironlog_wear_offline.db`)
- `wear/src/main/java/com/example/wear/data/WearRepository.kt`
- `wear/src/main/java/com/example/wear/sensors/HeartRateSensorMonitor.kt`
- `wear/src/main/java/com/example/wear/sync/WearDataListenerService.kt`
- `app/src/main/java/com/example/sync/WearSyncManager.kt`
- `app/src/main/java/com/example/sync/PhoneWearListenerService.kt`

---

## 1. Продуктовое описание

### Назначение модуля
Модуль `:wear` — это самостоятельное приложение для **Wear OS 3+** (`minSdk = 30`), позволяющее проводить силовые тренировки прямо с запястья без необходимости брать телефон в зал, с живым считыванием пульса и последующей автоматической или ручной синхронизацией со смартфоном.

### Экраны на часах
1. **Главный экран часов (`WearHomeScreen`):**
   - Адаптирован под круглые экраны (`TransformingLazyColumn` с `SurfaceTransformation`).
   - Кнопка быстрого старта свободной тренировки «Пустая тренировка / Empty Workout» (`wear_start_empty` -> `Quick Lift`).
   - Список синхронизированных программ с телефона (`wear_routine_<id>`).
   - Кнопка ручной синхронизации с телефоном «Синхронизация / Sync Phone».
2. **Активная тренировка (`WearActiveWorkoutPager`):**
   - Горизонтальный пейджер (`HorizontalPagerScaffold` + `HorizontalPager`) из `N` страниц упражнений + 1 итоговой страницы управления в конце.
   - **Страница упражнения:** название упражнения, таймер тренировки, живой пульс с датчика (`♥ bpm`), кнопка активного таймера отдыха с функцией `(Skip)`, номер текущего подхода (`Set X/Y`), ориентир из прошлой сессии (`Prev weight×reps`), крупные кнопки корректировки веса (`±2.5 кг`) и повторений (`±1`), кнопка **«Засчитать сет / Complete Set»** (`wear_complete_set` — запускает таймер отдыха и вибрацию) и кнопка **«+ Подход / + Add Set»**.
   - **Финальная страница управления:** крупный таймер длительности, кнопка Пауза/Продолжить (также дублируется аппаратной боковой кнопкой часов `KEYCODE_STEM_1`) и нижняя `EdgeButton` **«Завершить / Finish»** (`wear_finish_workout`).
3. **Итог тренировки (`WearSummaryScreen`):**
   - Подтверждение сохранения тренировки в локальную базу часов, счётчик выполненных подходов, средний пульс (если записывался) и кнопка `EdgeButton` «OK».

---

## 2. Техническое описание

### Автономное хранение на часах (`WearDatabase`)
- Отдельная локальная база Room `ironlog_wear_offline.db` с тремя таблицами на основе JSON-сериализации:
  - `wear_exercises` (предзаполняется из общего модуля `:core` — `ExerciseCatalogSeed`);
  - `wear_routines` (синхронизируются с телефона или берутся из стартового набора);
  - `wear_sessions` с флагом `pendingSync` для завершённых на часах тренировок, ещё не подтверждённых телефоном.

### Пульсометр (`HeartRateSensorMonitor`)
- Подключается к системному датчику `Sensor.TYPE_HEART_RATE` через `SensorManager`.
- Фильтрует некорректные значения датчика (принимает только диапазон `30..240 bpm`).
- При старте активной тренировки запрашивает разрешение на чтение пульса:
  - `Manifest.permission.BODY_SENSORS` на устройствах ниже API 36;
  - `android.permission.health.READ_HEART_RATE` на API 36+.
- Во время активной фазы (исключая паузу) накапливает замеры пульса в памяти, а при завершении тренировки агрегирует их в `avgHeartRate`, `maxHeartRate` и список точек `heartRateSamples`.

### Двусторонняя синхронизация через Wearable Data Layer
Обмен данными между модулями `:app` и `:wear` построен поверх Google Play Services Wearable Data Layer (`MessageClient` / `DataClient` / `NodeClient`) по фиксированным путям:
- `/ironlog/sync/phone_to_wear` — телефон отправляет актуальные программы, каталог упражнений и обновления;
- `/ironlog/sync/wear_to_phone` — часы отправляют завершённые сессии со статусом `pendingSync = 1`;
- `/ironlog/sync/request` — запрос на принудительную синхронизацию;
- `/ironlog/sync/ack` — ответ телефона `WearSyncAck` со списком `acknowledgedSessionIds`.

Оба сервиса (`WearSyncManager` в `:app` и `WearRepository` в `:wear`) перед обращением к `Wearable` API проверяют доступность Google Play Services (`GoogleApiAvailability.isGooglePlayServicesAvailable(...) == ConnectionResult.SUCCESS` и `checkApiAvailability(...)`). После получения `WearSyncAck` от `PhoneWearListenerService` часы снимают флаг `pendingSync = 0` для подтверждённых тренировок.
