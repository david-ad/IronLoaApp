package com.example.ironlog.core.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class MuscleGroup(
  val nameRu: String,
  val nameEn: String,
  val isFront: Boolean,
  val isBack: Boolean,
) {
  CHEST("Грудь", "Chest", isFront = true, isBack = false),
  BACK("Спина", "Back", isFront = false, isBack = true),
  SHOULDERS("Плечи", "Shoulders", isFront = true, isBack = true),
  BICEPS("Бицепс", "Biceps", isFront = true, isBack = false),
  TRICEPS("Трицепс", "Triceps", isFront = false, isBack = true),
  FOREARMS("Предплечья", "Forearms", isFront = true, isBack = true),
  QUADS("Квадрицепс", "Quads", isFront = true, isBack = false),
  HAMSTRINGS("Бицепс бедра", "Hamstrings", isFront = false, isBack = true),
  GLUTES("Ягодицы", "Glutes", isFront = false, isBack = true),
  CALVES("Икры", "Calves", isFront = false, isBack = true),
  CORE("Пресс и кор", "Core & Abs", isFront = true, isBack = false),
  FULL_BODY("Всё тело", "Full Body", isFront = true, isBack = true),
  CARDIO("Кардио", "Cardio", isFront = true, isBack = false);

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) nameRu else nameEn
}

@Serializable
enum class EquipmentType(val nameRu: String, val nameEn: String) {
  BARBELL("Штанга", "Barbell"),
  DUMBBELL("Гантели", "Dumbbell"),
  MACHINE("Тренажёр", "Machine"),
  CABLE("Блок / Кроссовер", "Cable"),
  BODYWEIGHT("Свой вес", "Bodyweight"),
  KETTLEBELL("Гиря", "Kettlebell"),
  BAND("Резинка", "Resistance Band"),
  CARDIO_MACHINE("Кардио-тренажёр", "Cardio Machine"),
  OTHER("Другое", "Other");

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) nameRu else nameEn
}

@Serializable
enum class ExerciseCategory(val nameRu: String, val nameEn: String) {
  COMPOUND("Базовое", "Compound"),
  ISOLATION("Изолирующее", "Isolation"),
  OLYMPIC("Тяжёлая атлетика", "Olympic"),
  PLYOMETRIC("Плиометрика", "Plyometric"),
  ISOMETRIC("Статика", "Isometric"),
  CARDIO("Кардио", "Cardio");

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) nameRu else nameEn
}

@Serializable
enum class MeasurementType(val nameRu: String, val nameEn: String) {
  WEIGHT_REPS("Вес + повторения", "Weight + Reps"),
  REPS_ONLY("Только повторения", "Reps Only"),
  DURATION("Время", "Duration"),
  WEIGHT_DURATION("Вес + время", "Weight + Duration"),
  DISTANCE_DURATION("Дистанция + время", "Distance + Duration");

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) nameRu else nameEn
}

@Serializable
enum class SetType(val shortCode: String, val nameRu: String, val nameEn: String) {
  NORMAL("N", "Обычный", "Normal"),
  WARMUP("W", "Разминочный", "Warm-up"),
  FAILURE("F", "До отказа", "Failure"),
  DROP("D", "Дроп-сет", "Drop Set");

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) nameRu else nameEn
}

@Serializable
enum class WeightUnit(val symbolRu: String, val symbolEn: String) {
  KG("кг", "kg"),
  LBS("фнт", "lbs");

  fun symbol(lang: AppLanguage): String = if (lang == AppLanguage.RU) symbolRu else symbolEn

  fun toDisplay(kg: Double): Double =
    if (this == KG) kg else (kg * KG_TO_LBS)

  fun toKg(displayValue: Double): Double =
    if (this == KG) displayValue else (displayValue / KG_TO_LBS)

  fun format(kg: Double): String {
    val value = toDisplay(kg)
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
  }

  companion object {
    const val KG_TO_LBS = 2.20462262
  }
}

@Serializable
enum class AppLanguage {
  RU,
  EN,
}

@Serializable
enum class ThemeMode {
  DARK,
  LIGHT,
  SYSTEM,
}

@Serializable
enum class OneRmFormula(val labelRu: String, val labelEn: String) {
  EPLEY("Эпли (Epley)", "Epley"),
  BRZYCKI("Бжицки (Brzycki)", "Brzycki"),
  LOMBARDI("Ломбарди (Lombardi)", "Lombardi"),
  OCONNER("О'Коннер (O'Conner)", "O'Conner");

  fun label(lang: AppLanguage): String = if (lang == AppLanguage.RU) labelRu else labelEn
}

@Serializable
enum class IllustrationPose {
  BENCH_PRESS,
  SQUAT,
  DEADLIFT,
  OVERHEAD_PRESS,
  ROW,
  PULL_UP,
  CURL,
  EXTENSION,
  LUNGE,
  PLANK,
  CARRY,
  CARDIO,
}
