class SemanticAnalyzer(private val tokens: List<Token>) {

    private val symbolTable = mutableMapOf<String, String>()

    fun analyze(): List<String> {
        val logs = mutableListOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            when (token.value) {
                "dim" -> index = handleDeclaration(index, logs)
                "let" -> index = handleAssignment(index, logs)
                "if" -> index = handleCondition(index + 1, logs)
                "for", "while" -> index = handleLoop(index, logs)
                else -> index++
            }
        }
        return logs
    }

    private fun handleDeclaration(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index + 1
        val variables = mutableListOf<String>()
        var type: String? = null

        while (currentIndex < tokens.size) {
            val currentToken = tokens[currentIndex]
            when {
                currentToken.type == TokenType.IDENTIFIER -> variables.add(currentToken.value)
                listOf("%", "!", "$").contains(currentToken.value) -> {
                    type = currentToken.value
                    break
                }
                currentToken.value == "," -> {} // Просто продолжаем
                currentToken.value == ";" -> break
                else -> {
                    logs.add("Ошибка: неверный синтаксис объявления переменной на токене '${currentToken.value}'.")
                    return currentIndex
                }
            }
            currentIndex++
        }

        if (type == null) {
            logs.add("Ошибка: не указан тип данных для переменных.")
            return currentIndex
        }

        for (variable in variables) {
            if (symbolTable.containsKey(variable)) {
                logs.add("Ошибка: переменная $variable уже описана!")
            } else {
                symbolTable[variable] = type
                logs.add("Переменная $variable успешно описана с типом $type.")
            }
        }
        return currentIndex + 1
    }

    private fun handleAssignment(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index + 1

        if (currentIndex >= tokens.size || tokens[currentIndex].type != TokenType.IDENTIFIER) {
            logs.add("Ошибка: ожидалась переменная после 'let'.")
            return currentIndex
        }

        val variable = tokens[currentIndex].value
        currentIndex++

        if (!symbolTable.containsKey(variable)) {
            logs.add("Ошибка: переменная $variable не описана!")
            return currentIndex
        }

        if (currentIndex >= tokens.size || tokens[currentIndex].value != "=") {
            logs.add("Ошибка: ожидался оператор '='.")
            return currentIndex
        }
        currentIndex++

        val (expressionType, newIndex) = analyzeExpression(currentIndex, logs)
        currentIndex = newIndex

        val variableType = symbolTable[variable]

        logs.add("Присваивание: переменная $variable (тип $variableType), значение выражения (тип $expressionType)")

        if (expressionType != variableType) {
            logs.add("Ошибка: несовместимые типы: переменной $variable (тип $variableType) присваивается значение типа $expressionType.")
        } else {
            logs.add("Присваивание переменной $variable успешно.")
        }



        return currentIndex
    }
    private fun handleLoop(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index + 1

        // Ожидаем открывающую скобку (
        if (currentIndex >= tokens.size || tokens[currentIndex].value != "(") {
            logs.add("Ошибка: ожидалась '(' после 'for'.")
            return currentIndex
        }
        currentIndex++

        // Пропускаем выражение инициализации
        currentIndex = skipExpression(currentIndex)

        // Ожидаем точку с запятой
        if (currentIndex >= tokens.size || tokens[currentIndex].value != ";") {
            logs.add("Ошибка: ожидалась ';' после инициализации итератора.")
            return currentIndex
        }
        currentIndex++

        // Пропускаем условие
        currentIndex = skipExpression(currentIndex)

        // Ожидаем точку с запятой
        if (currentIndex >= tokens.size || tokens[currentIndex].value != ";") {
            logs.add("Ошибка: ожидалась ';' после условия цикла.")
            return currentIndex
        }
        currentIndex++

        // Пропускаем шаг итерации
        currentIndex = skipExpression(currentIndex)

        // Ожидаем закрывающую скобку )
        if (currentIndex >= tokens.size || tokens[currentIndex].value != ")") {
            logs.add("Ошибка: ожидалась ')' после заголовка цикла.")
            return currentIndex
        }
        currentIndex++

        // Ожидаем открывающую фигурную скобку {
        if (currentIndex >= tokens.size || tokens[currentIndex].value != "{") {
            logs.add("Ошибка: ожидалась '{' перед телом цикла.")
            return currentIndex
        }
        currentIndex++

        // Пропускаем тело цикла
        while (currentIndex < tokens.size && tokens[currentIndex].value != "}") {
            currentIndex++
        }

        // Ожидаем закрывающую фигурную скобку }
        if (currentIndex >= tokens.size || tokens[currentIndex].value != "}") {
            logs.add("Ошибка: ожидалась '}' в конце тела цикла.")
            return currentIndex
        }
        currentIndex++

        logs.add("Цикл успешно обработан.")
        return currentIndex
    }



    private fun skipExpression(currentIndex: Int): Int {
        var index = currentIndex
        while (index < tokens.size && tokens[index].value != ";" && tokens[index].value != ")" && tokens[index].value != "{") {
            index++
        }
        return index
    }




    private fun handleCondition(index: Int, logs: MutableList<String>): Int {
        // Получаем тип выражения и новый индекс
        val (conditionType, newIndex) = analyzeExpression(index, logs)

        // Проверяем тип условия
        if (conditionType != "$") {
            logs.add("Ошибка: условие должно быть булевым, но получен тип $conditionType.")
        } else {
            logs.add("Условие корректно.")
        }

        // Возвращаем обновленный индекс
        return newIndex
    }



    private fun analyzeExpression(startIndex: Int, logs: MutableList<String>): Pair<String, Int> {
        var currentIndex = startIndex
        var type: String? = null

        while (currentIndex < tokens.size) {
            val currentToken = tokens[currentIndex]
            when (currentToken.type) {
                TokenType.IDENTIFIER -> {
                    val variable = currentToken.value
                    if (!symbolTable.containsKey(variable)) {
                        logs.add("Ошибка: переменная $variable не описана!")
                        return Pair("unknown", currentIndex)
                    }
                    if (type == null) {
                        type = symbolTable[variable]
                    } else if (type != symbolTable[variable]) {
                        logs.add("Ошибка: несовместимые типы в выражении: $type и ${symbolTable[variable]}.")
                        return Pair("unknown", currentIndex)
                    }
                }
                TokenType.NUMBER -> {
                    // Получаем тип числа из значения токена
                    val tokenType = if (currentToken.value.contains(".")) "!" else "%"
                    if (type == null) {
                        type = tokenType
                    } else if (type != tokenType) {
                        logs.add("Ошибка: несовместимые типы в выражении: $type и $tokenType.")
                        return Pair("unknown", currentIndex)
                    }
                }

                TokenType.KEYWORD -> {
                    if (listOf("true", "false").contains(currentToken.value)) {
                        if (type == null) {
                            type = "$"
                        } else if (type != "$") {
                            logs.add("Ошибка: булевое значение не соответствует типу $type.")
                            return Pair("unknown", currentIndex)
                        }
                    }
                }

                TokenType.SYMBOL -> {
                    if (currentToken.value in listOf("+", "-", "*", "/")) {
                        if (type !in listOf("%", "!")) {
                            logs.add("Ошибка: операции ${currentToken.value} недопустимы для типа $type.")
                            return Pair("unknown", currentIndex)
                        }
                    } else if (currentToken.value in listOf("and", "or", "not")) {
                        if (type != "$") {
                            logs.add("Ошибка: операции ${currentToken.value} недопустимы для типа $type.")
                            return Pair("unknown", currentIndex)
                        }
                    } else {
                        break
                    }
                }
                else -> break
            }
            currentIndex++
        }
        return Pair(type ?: "unknown", currentIndex)
    }


}
