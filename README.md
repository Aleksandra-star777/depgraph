# depgraph — инструмент визуализации графа зависимостей

Учебный проект по дисциплине «Конфигурационное управление».
Вариант №10. Этап 1 — минимальный CLI-прототип с конфигурацией.

## Описание

CLI-приложение на Java, которое принимает параметры командной строки для настройки анализа зависимостей пакета и выводит их в формате «ключ = значение». На текущем этапе граф не строится — реализована только конфигурация и её валидация.

## Параметры командной строки

| Опция | Обязательно | По умолчанию | Описание |
|---|---|---|---|
| `--package` | да | — | Имя анализируемого пакета |
| `--repo` | да | — | Путь к файлу репозитория или URL |
| `--mode` | нет | `local` | Режим: `local` или `remote` |
| `--version` | нет | `latest` | Версия пакета |
| `--output` | нет | `graph.png` | Имя файла графа (.png, .svg, .dot) |
| `--depth` | нет | `3` | Максимальная глубина анализа (1..100) |

## Требования

- JDK 17 или выше.
- Windows — для готового `run.bat`.

`run.bat` сам находит JDK: проверяет `JAVA_HOME`, папку `.jdks` от IntelliJ, стандартные пути `C:\Program Files\Java\jdk*` и `C:\Program Files\Eclipse Adoptium\jdk*`.

## Сборка и запуск

### Автоматически (Windows)

Запустить двойным кликом `run.bat`. Скрипт:
1. Найдёт JDK.
2. Скомпилирует `Main.java` в папку `out`.
3. Прогонит 4 примера: успешный запуск и 3 варианта ошибок.

### Вручную

```
mkdir out
javac -encoding UTF-8 -d out src/ru/miem/depgraph/Main.java
java -cp out ru.miem.depgraph.Main --package requests --repo ./repo.json --mode local --version 2.31.0 --output deps.png --depth 5
```

## Примеры

### Успешный запуск

```
java -cp out ru.miem.depgraph.Main --package requests --repo ./repo.json --mode local --version 2.31.0 --output deps.png --depth 5
```

```
=== Параметры запуска ===
package = requests
repo    = ./repo.json
mode    = local
version = 2.31.0
output  = deps.png
depth   = 5
=========================
```

### Ошибка: не задано имя пакета

```
java -cp out ru.miem.depgraph.Main
```

```
Ошибка: Не задано имя анализируемого пакета (--package)
```

### Ошибка: локальный репозиторий не найден

```
java -cp out ru.miem.depgraph.Main --package requests --repo ./missing.json --mode local
```

```
Ошибка: Локальный репозиторий не найден: ./missing.json
```

### Ошибка: неверная глубина

```
java -cp out ru.miem.depgraph.Main --package requests --repo ./repo.json --depth abc
```

```
Ошибка: Глубина должна быть целым числом: abc
```

### Ошибка: неизвестный режим

```
java -cp out ru.miem.depgraph.Main --package requests --repo ./repo.json --mode unknown
```

```
Ошибка: Недопустимый режим: unknown. Допустимые: [local, remote]
```

## Тесты

Папка `tests/` зарезервирована под тесты (пока содержит заглушку `.gitkeep`).

## Автор

Студентка группы ИКБО-14-25, Замуруева Александра.