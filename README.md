# Danger Sense

Fabric-мод, который предупреждает игрока об опасных ситуациях (крипер, TNT,
Страж, здоровье, воздух, лава, падение) до того, как они станут смертельными.
**Никогда не блокирует урон** — только виньетка, звук сердцебиения и пульсация.

Собирается через ваш **FigureStonePlugin** (наследник MossyPlugin), на
**official Mojang mappings** (не Yarn) — как задумано в новой,
пост-ребрендинговой схеме вашей экосистемы.

## Структура пакетов

```
com.darkz.dangersense
├── DangerSenseMod          — common entrypoint, регистрация звука, загрузка конфига
├── client/
│   ├── DangerSenseClient   — client entrypoint, регистрация HUD/tick колбэков
│   └── ClientTickHandler   — связывает WarningManager, HeartbeatPlayer, VignetteOverlay
├── config/
│   ├── DangerSenseConfig   — POJO для Gson
│   └── ConfigManager       — load/save JSON + опциональный hot reload
├── danger/
│   ├── DangerLevel         — NONE/LOW/MEDIUM/HIGH/CRITICAL с базовой интенсивностью
│   ├── DangerWarning       — record: итог одного тика (source, level, intensity)
│   └── WarningManager      — тикает все детекторы, выбирает самый сильный
├── detector/
│   ├── DangerDetector      — общий интерфейс (shouldWarn + getLevel/getIntensity)
│   ├── CreeperDetector, TntDetector, WardenDetector,
│   │   LowHealthDetector, DrowningDetector, LavaDetector, FallDetector
├── render/
│   ├── VignetteOverlay     — процедурная виньетка по краям экрана (без текстур)
│   └── PulseAnimation      — синусоидальная пульсация альфы
├── sound/
│   └── HeartbeatPlayer     — воспроизведение heartbeat с интервалом от интенсивности
└── util/
    ├── RaycastUtil         — line-of-sight без лишних аллокаций
    └── TickThrottle        — «раз в N тиков» без лишней логики
```

Пакет — `com.darkz.dangersense` (как `com.darkz.skintotem` в вашем реальном
SkinTotem), не `com.darkz70.*`.

## Сборка (FigureStonePlugin)

Всё Gradle-обвязка идёт через ваш плагин `net.darkz70.figurestone-plugin-*`
(модули `settings` и `core`), а не через голый `fabric-loom`:

- **`settings.gradle`** — применяет `net.darkz70.figurestone-plugin-settings`.
  Он сам создаёт stonecutter-подпроекты `fabric-<version>` из
  `fabric.multi_versions` в корневом `gradle.properties`, сам генерирует/
  обновляет `versions/fabric-<version>/gradle.properties` (там нужен только
  `build.fabric_api` — плагин заполняет его сам при пустом файле).
- **`build.gradle`** (корневой, применяется к каждой версии-подпроекту) —
  подключает `net.darkz70.figurestone-plugin-core` и настраивает блок
  `figurestoneDependencies { minecraft, fabricApi, fabricLoader, lombok }`.
  FigureStoneLib подключена отдельно (она не идёт через `dep.*`-механизм
  плагина — это для Modrinth-модов, а FigureStoneLib у вас в приватном
  GitHub Packages).
- **`gradle.properties`** — схема `data.*` / `mod_loaders` / `fabric.multi_versions`,
  как в вашем архиве `FigureStonePlugin_repository.zip`.
- Папки версий — **`versions/fabric-1.20.1`**, а не `versions/1.20.1`
  (имя проекта в Stonecutter = `<loader>-<version>`).

### ✅ Версия плагина

`settings.gradle`/`build.gradle` используют `version "1.2.0"` — подтверждено
вами напрямую (в присланном архиве `FigureStonePlugin_repository.zip` была
зашита другая, `3.7.0-beta.19` — видимо, устаревший снапшот).

### ✅ Модуль `figurestone-plugin-stonecutter` теперь тоже подключён

Во втором архиве нашёлся модуль `stonecutter` (в первом его не было). Он даёт
агрегирующие задачи `buildAndCollect+<loader>+All`, `buildAndCollect+All`,
`publish+All` и т.д. — именно `buildAndCollect+All` вызывается в вашем
реальном `build.yml` SkinTotem (`./gradlew buildAndCollect+All || ...`).
Применён в том же `build.gradle`, что и `-core`: при
`dev.kikugie.stonecutter.hard_mode=true` корневой проект — это и есть
текущая активная версия, так что один файл одновременно работает и как
per-версия скрипт, и как корневой (с видимостью дочерних проектов).

`buildAndCollect` (per-версия, из `-core`) копирует готовый jar в корневую
`libs/` — `.github/workflows/build.yml` обновлён под это (`path: libs/*.jar`
вместо `versions/fabric-*/build/libs/*.jar`).

## ⚠️ Главное: official Mojang mappings, не Yarn

`FabricLoaderManager` (core-модуль плагина) подключает маппинги так:
```java
dependencies.add("mappings", loom.officialMojangMappings());
```
Поэтому весь Java-код здесь написан на именах классов/методов Mojang
(`Player`, `ClientLevel`, `Creeper`, `PrimedTnt`, `Warden`, `getDeltaMovement()`,
`onGround()`, `distanceToSqr()` и т.д.), а не на Yarn (`PlayerEntity`,
`ClientWorld`, `squaredDistanceTo()` и т.д.).

**Часть имён методов я не могу подтвердить без доступа к реальным mappings
или собранному проекту** — такие места помечены прямо в коде комментарием
`⚠ MOJMAP UNCERTAINTY` / `⚠ MOJMAP:`. Список самых рискованных:

| Файл | Место | Риск |
|---|---|---|
| `DangerSenseMod.java` | `SoundEvent.createVariableRangeEvent(...)`, `new ResourceLocation(...)` | средний — конструктор `ResourceLocation` мог стать deprecated/приватным в поздних 1.21.x |
| `CreeperDetector.java` | `creeper.getSwellFactor(0f)` | высокий — не уверен в точном имени; fallback указан в комментарии (`isIgnited()`) |
| `TntDetector.java` | `tnt.getFuse()` | низкий-средний |
| `FallDetector.java` | `result.getLocation()` | средний |
| `HeartbeatPlayer.java` | `player.level()` | средний |
| `RaycastUtil.java`, `FallDetector.java` | `ClipContext.Block` / `ClipContext.Fluid` | средний |

Остальные замены (`getHealth()`, `getAirSupply()`, `getDeltaMovement()`,
`onGround()`, `distanceToSqr()`, `getEntitiesOfClass()`, `inflate()`,
`position()`, `blockPosition()`, `getEyePosition()`, `BlockPos.containing()`,
`fluidState.is()`, `guiWidth()/guiHeight()`) — уверенность выше, но тоже не
проверено компиляцией. **Это неизбежно потребует одного прогона CI и правки
по логам ошибок** — так же, как вы обычно и работаете с новыми API.

## Java-версия по MC-версии (авто, из вашего плагина)

`FigureStonePluginCore.getJavaVersion()` сам выбирает JDK по версии MC:
`< 1.16.5 → 8`, `< 1.18 → 16`, `< 1.20.5 → 17`, `< 26.1 → 21`, `≥ 26.1 → 25`.
То есть для всего диапазона Danger Sense (1.20.1–1.21.11) реально будет
использоваться **JDK 21**, а не 25 — плагин выберет его сам, вне зависимости
от того, что было в исходном ТЗ.

## Производительность

- Детекторы вызываются не каждый тик, а раз в `scanIntervalTicks` (по
  умолчанию 4) через `TickThrottle` в `WarningManager`.
- Поиск сущностей — `getEntitiesOfClass` с ограниченным `AABB` вокруг игрока,
  без сканирования мира целиком.
- `RaycastUtil` — один `ClipContext` без промежуточных списков.
- Виньетка рисуется процедурно `fillGradient`, без текстур.

## Конфиг

`config/dangersense.json` создаётся автоматически при первом запуске
(эталон значений — `config-default.json` в корне). Хот-релоад
(`hotReloadEnabled`) проверяет файл не каждый тик, а примерно раз в секунду.

## FigureStoneLib — обязательная зависимость

FigureStoneLib подключена не опционально, а как **жёсткая зависимость**:

- `build.gradle`: `modImplementation "com.darkz:figurestonelib:${prop("dep.figurestonelib")}"`
- `gradle.properties`: `dep.figurestonelib = 1.0.0` (версия — поставьте актуальную)
- `fabric.mod.json`: `"figurestonelib": ">=${figurestonelib}"` в `depends` —
  без неё игра не запустит мод (Fabric Loader покажет экран с недостающей
  зависимостью).

⚠ У меня есть исходники **FigureStonePlugin** (build-плагин), но не самой
**FigureStoneLib** (библиотеки) — поэтому я не могу подставить в код реальные
вызовы её API (методы, классы), только оставить комментарии-точки интеграции
ниже. Если пришлёте архив с её исходниками — подключу по-настоящему, а не
абстрактно.

Точки, где FigureStoneLib напрашивается:
1. **`config/ConfigManager`** — если в библиотеке есть общий
   `JsonConfig<T>`/file-watcher, замените ручной Gson I/O на него.
2. **`util/RaycastUtil`** — если там уже есть raycast/line-of-sight утилиты.
3. **Логирование** — `DangerSenseMod.LOGGER` можно заменить на общий логер.

## Gradle Wrapper

`gradlew` / `gradlew.bat` / `gradle-wrapper.properties` (Gradle 8.14) и
`gradle-wrapper.jar` — всё включено (jar — тот, что вы загрузили,
скрипты — скопированы из вашего реального SkinTotem).

## GitHub Actions

`.github/workflows/build.yml`: JDK 21 (проверенный вариант, см. раздел про
Java-версию выше), `chmod +x ./gradlew`, `./gradlew build`. Секрет
`GH_PACKAGES_TOKEN` нужен и для FigureStoneLib, и для самого FigureStonePlugin
(`settings.gradle` его тоже использует) — убедитесь, что у него хватает прав
на чтение обоих приватных пакетов.

## Чего не хватает / не проверено

- Все места `⚠ MOJMAP` в коде (таблица выше) — первый прогон CI покажет,
  что чинить.
- Реальные вызовы FigureStoneLib (см. раздел выше) — есть только исходники
  build-плагина, не самой библиотеки.
