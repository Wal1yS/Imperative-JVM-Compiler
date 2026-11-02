# Semantic Analyzer — часть Участника 2: система типов, Type Checker, проверка границ массивов

**Проект:** Imperative JVM Compiler (Language I, диалект команды Vitebsk)
**Автор части:** Тимур Бикметов (Участник 2)
**Ветка:** `feat/semantic-analyzer-pass2` (основана на `feat/semantic-analyzer-pass1`)
**Статус:** реализовано. Все 15 примеров из `examples/` проходят анализ; 42 теста на типы и 8 тестов на границы массивов в `SemanticTestRunner` проходят.

---

## 1. Место в конвейере

```
Parser → AST → Pass 1: DeclarationAndScopeChecker (Участник 1)
             → Pass 2: TypeChecker            (Участник 2)
             → Pass 3: ArrayBoundsChecker     (Участник 2)
             → оптимизации                    (Участник 3)
```

Все проверки этой части **не изменяют AST**: дерево только аннотируется (AST → Annotated AST, лекция 7).
После Pass 2 в `SemanticContext` для каждого выражения есть его семантический тип (`getExpressionType`),
а для каждой переменной, параметра и переменной цикла — объявленный или выведенный тип (`getSymbolType`).
Эти аннотации используют Pass 3, оптимизации и будущая генерация JVM-кода.

Каждый проход запускается, только если в предыдущем не было ошибок.

## 2. Модель типов (`ru.innopolis.semantic.type`)

| Класс | Назначение |
|---|---|
| `PrimitiveType` | синглтоны `integer`, `real`, `boolean`, `string`, сравниваются по ссылке |
| `ArrayType` | тип элемента + размер, известный при компиляции (или `null`); флаг `sized` отличает `array T` без размера |
| `RecordType` | поля в порядке объявления; один объект на одно объявление record |
| `VoidType` | «тип» подпрограммы без возвращаемого типа |
| `ErrorType` | тип ошибочного выражения, совместим со всем |
| `TypeResolver` | `TypeNode` → `Type`, с кэшем: имя типа — это синоним, record — всегда один и тот же объект |
| `TypeSystem` | правила присваивания, `as`, операторов |
| `ConstantEvaluator` | вычисляет константные выражения, **не меняя AST** (для размеров и индексов массивов) |

**Подавление наведённых ошибок (лекция 7, слайд 6).** Выражение с ошибкой получает `ErrorType`, а все правила
считают `ErrorType` допустимым. Поэтому одна ошибка даёт одно сообщение:

```
var a is (1 + "a") * 2     // Operator '+' cannot be applied to integer and string
var b is a + 1             // ошибок нет: a имеет ErrorType
```

## 3. Правила типов

**Присваивание, инициализация, передача аргументов, `return`.** По спецификации передача аргумента — это присваивание,
поэтому для всех четырёх случаев действует одно правило `TypeSystem.isAssignable`:
- типы должны совпадать, с одним исключением: **integer неявно расширяется до real** (как `i2d` в JVM);
- boolean ↔ число и real → integer неявно **запрещены** (Vitebsk: «No Implicit Casts»);
- record: только тот же самый тип, то есть совпадение по имени объявления («full name identity» в спецификации);
- массивы сравниваются по структуре: совпадает тип элемента, и размеры равны, если оба известны.
  Поэтому `type Small is array [4] integer` можно передать в параметр `arr: array integer` (пример 10).

Почему int → real разрешено: собственные тесты команды (примеры 2 и 13) содержат `(count as real) / 2`
и `(total as real) / s.grades.size`, а презентация обосновывает запрет неявных приведений тем, что в JVM
boolean и числа разделены. Если команда решит запретить и это, меняется одна строка в `isAssignable`.

**Явное приведение `as`** (`TypeSystem.canCast`). Презентация Vitebsk требует, чтобы `as` следовал
таблице conformance из спецификации:

| из \ в | integer | real | boolean |
|---|---|---|---|
| integer | да | да | да (0/1, иначе ошибка выполнения) |
| real | да (округление) | да | **нет** |
| boolean | да | да | да |

`string` приводится только к `string`. Массивы и record не приводятся никогда, а целью `as` может быть только примитивный тип.

**Операторы** (`binaryResult`, `unaryResult`):

| Операторы | Операнды | Результат |
|---|---|---|
| `+ - * /` | числа | integer, если оба integer, иначе real |
| `%` | integer, integer | integer |
| `and or xor`, `not` | boolean | boolean |
| `< <= > >=` | числа | boolean |
| `= /=` | числа, или два boolean, или две string | boolean |
| унарные `+ -` | число | тот же тип |

Арифметика со `string` запрещена.

## 4. Проверки Pass 2: `TypeChecker`

| № | Проверка | Пример → сообщение |
|---|---|---|
| C3 | Инициализатор соответствует типу | `var r: integer is true` → `Cannot initialize variable 'r' of type integer with a value of type boolean` |
| C3 | Вывод типа `var x is e` | `var squares is buildSquares(5)` → `squares: array integer` |
| C3 | Присваивание | `i := 2.5` → `Type mismatch in assignment: cannot assign real to integer` |
| C3 | Число и типы аргументов | `f(1)` → `Routine 'f' expects 2 argument(s), but 1 were given`; `f(1, true)` → `Argument 2 of routine 'f': cannot pass boolean as integer` |
| C3 | Процедура без типа не может быть значением (лекция 7, слайд 5) | `var x is p()` → `Routine 'p' has no return type, so its call cannot be used as a value` |
| C3 | Имя подпрограммы или типа вместо переменной | `print f` → `'f' is a routine, not a variable; call it as f(...)` |
| C3 | Условие `if`/`while` — boolean | `if 1 then` → `Condition of 'if' must be boolean, got integer` |
| C3 | Операторы | `true + 1` → `Operator '+' cannot be applied to boolean and integer` |
| V1 | `return` в процедуре без значения, в функции со значением нужного типа | `routine p() is return 1 end` → `... cannot return a value`; `return` в функции → `must return a value of type integer`; `return true` → `expected integer, got boolean` |
| V1 | Функция возвращает значение на всех путях | `if x > 0 then return 1 end` → `Routine 'f' must return a value of type integer on every path` |
| V1 | Тело `=> Expression` | `routine f(x: integer): boolean => x + 1` → `... its expression body has type integer` |
| V3 | Нет неявных приведений | `var b: boolean is 1` → ошибка |
| V4 | Допустимость `as` | `3.5 as boolean` → `Cannot convert real to boolean with 'as'`; `1 as array integer` → `'as' converts only to primitive types` |
| V5 | Правила `string` | `1 + "a"`, `s * s` → ошибка оператора; `print p` (record) → `Cannot print a value of type P` |
| V8 | Сигнатура forward-объявления совпадает с полным объявлением по типам | `routine f(x: integer): integer` + `routine f(x: real) ...` → `Parameter 1 of routine 'f' has type real, but its forward declaration says integer` |
| V9 | Тип переменной цикла | `for i in a .. b`: границы integer, `i: integer`; `for v in arr`: `v` получает тип элемента; `for v in 5` → `requires an array, got integer` |
| V10 | `.size` у массива | `arr.size` → integer; `arr.foo` → `Arrays have no member 'foo' (only 'size')`; `arr.size := 5` → `Cannot assign to read-only property 'size' of an array` |
| — | Поля record | `p.y` → `Record type 'P' has no field 'y'` |
| — | Массивы | индекс не integer, индексирование не-массива, `var a: array integer` без размера (разрешено только параметрам и возвращаемому типу), размер `<= 0` или не integer |
| — | Рекурсивные record | `type N is record var next: N end` → `Recursive type definition 'N' is not supported` |

## 5. Проверки Pass 3: `ArrayBoundsChecker` (C4, V6)

Массивы индексируются с нуля, допустимые индексы — `0 .. size - 1`. Индекс проверяется, когда его значения
известны при компиляции:
- константа или константное выражение: `a[10]` при `array [10]` → `index is 10, array size is 10 (valid indices are 0..9)`,
  `a[2 + 3]` при `[5]`, `a[-1]`;
- выражение от переменных `for` с константными границами (переменную цикла нельзя изменить,
  поэтому её диапазон точно известен):
  ```
  for i in 1 .. 10 loop a[i] := i end     // index takes values 1..10, array size is 10 (valid indices are 0..9)
  for i in 0 .. 9 loop print a[i + 1] end  // index takes values 1..10
  ```
  Это ловит типичную ошибку «индексация с 1» после перехода на 0-based массивы.
- Внутри `if`/`while` диапазон цикла не учитывается: индекс может быть защищён условием,
  например `if i > 0 then a[i - 1]`. Индексы, известные только во время выполнения, не проверяются.

## 6. Правки Pass 1 (отдельный коммит)

- Поля record объявляются в собственном скоупе. Раньше они попадали в объемлющий: два record с полем `x`
  давали ошибку дубликата, а `x` в `main` находился как поле.
- Типы, объявленные внутри подпрограмм (`type T is ...`), теперь регистрируются.
- `SemanticContext.declaredSymbols` связывает объявляющий узел с его символом.

## 7. Как запустить

```bash
mvn -q compile
java -cp target/classes ru.innopolis.SemanticTestRunner
java -cp target/classes ru.innopolis.Main examples/13.txt
```
