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
                "do" -> index = handleDoWhile(index, logs)
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
                currentToken.value == "," -> {}
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
    private fun handleDoWhile(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index + 1

        logs.add("Обработка цикла 'do while'.")
        val bodyStart = currentIndex
        currentIndex = skipBody(currentIndex, logs)

        if (currentIndex >= tokens.size || tokens[currentIndex].value != "while") {
            logs.add("Ошибка: ожидалось 'while' после тела цикла 'do'.")
            return currentIndex
        }
        currentIndex++

        val (conditionType, conditionIndex) = analyzeExpression(currentIndex, logs)
        currentIndex = conditionIndex

        if (conditionType != "$") {
            logs.add("Ошибка: условие в 'do while' должно быть булевым (тип $), но получен тип $conditionType.")
            return currentIndex
        }

        if (currentIndex >= tokens.size || tokens[currentIndex].value != "loop") {
            logs.add("Ошибка: ожидалось 'loop' после условия цикла 'do while'.")
            return currentIndex
        }
        currentIndex++

        logs.add("Цикл 'do while' успешно обработан: тело [$bodyStart-${currentIndex - 1}], условие [$conditionType].")
        return currentIndex
    }
    private fun skipBody(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index

        if (currentIndex < tokens.size && tokens[currentIndex].value == "{") {
            currentIndex++
            while (currentIndex < tokens.size && tokens[currentIndex].value != "}") {
                currentIndex++
            }
            if (currentIndex >= tokens.size || tokens[currentIndex].value != "}") {
                logs.add("Ошибка: ожидалась '}' в конце тела цикла.")
                return currentIndex
            }
            currentIndex++
        } else {
            logs.add("Ошибка: ожидалось тело цикла, начинающееся с '{'.")
        }

        return currentIndex
    }

    private fun handleLoop(index: Int, logs: MutableList<String>): Int {
        var currentIndex = index + 1

        if (currentIndex >= tokens.size || tokens[currentIndex].value != "(") {
            logs.add("Ошибка: ожидалась '(' после 'for'.")
            return currentIndex
        }
        currentIndex++

        currentIndex = skipExpression(currentIndex)

        if (currentIndex >= tokens.size || tokens[currentIndex].value != ";") {
            logs.add("Ошибка: ожидалась ';' после инициализации итератора.")
            return currentIndex
        }
        currentIndex++

        currentIndex = skipExpression(currentIndex)

        if (currentIndex >= tokens.size || tokens[currentIndex].value != ";") {
            logs.add("Ошибка: ожидалась ';' после условия цикла.")
            return currentIndex
        }
        currentIndex++

        currentIndex = skipExpression(currentIndex)

        if (currentIndex >= tokens.size || tokens[currentIndex].value != ")") {
            logs.add("Ошибка: ожидалась ')' после заголовка цикла.")
            return currentIndex
        }
        currentIndex++

        if (currentIndex >= tokens.size || tokens[currentIndex].value != "{") {
            logs.add("Ошибка: ожидалась '{' перед телом цикла.")
            return currentIndex
        }
        currentIndex++

        while (currentIndex < tokens.size && tokens[currentIndex].value != "}") {
            currentIndex++
        }

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
        val (conditionType, newIndex) = analyzeExpression(index, logs)

        if (conditionType != "$") {
            logs.add("Ошибка: условие должно быть булевым, но получен тип $conditionType.")
        } else {
            logs.add("Условие корректно.")
        }

        return newIndex
    }

    private fun analyzeExpression(startIndex: Int, logs: MutableList<String>): Pair<String, Int> {
        var currentIndex = startIndex
        var typeStack = mutableListOf<String?>()
        var operatorStack = mutableListOf<String>()

        fun getType(): String? = if (typeStack.isNotEmpty()) typeStack.last() else null

        while (currentIndex < tokens.size) {
            val currentToken = tokens[currentIndex]

            when (currentToken.type) {
                TokenType.IDENTIFIER -> {
                    val variable = currentToken.value
                    if (!symbolTable.containsKey(variable)) {
                        logs.add("Ошибка: переменная $variable не описана!")
                        return Pair("unknown", currentIndex)
                    }
                    val varType = symbolTable[variable]
                    typeStack.add(varType)
                }
                TokenType.NUMBER -> {
                    val tokenType = if (currentToken.value.contains(".")) "!" else "%"
                    typeStack.add(tokenType)
                }
                TokenType.KEYWORD -> {
                    if (currentToken.value in listOf("true", "false")) {
                        typeStack.add("$")
                    } else if (currentToken.value in listOf("and", "or", "not")) {
                        operatorStack.add(currentToken.value)
                    } else {
                        break
                    }
                }
                TokenType.SYMBOL -> {
                    val symbol = currentToken.value
                    if (symbol == "(") {
                        val (innerType, newIndex) = analyzeExpression(currentIndex + 1, logs)
                        currentIndex = newIndex
                        typeStack.add(innerType)
                        if (currentIndex >= tokens.size || tokens[currentIndex].value != ")") {
                            logs.add("Ошибка: ожидалась ')'.")
                            return Pair("unknown", currentIndex)
                        }
                    } else if (symbol == ")") {
                        return Pair(getType() ?: "unknown", currentIndex)
                    } else if (symbol in listOf("+", "-", "*", "/")) {
                        operatorStack.add(symbol)
                    } else if (symbol in listOf("<", ">", "=", "!=", "<=", ">=")) {
                        operatorStack.add(symbol)
                    } else {
                        break
                    }
                }
                else -> break
            }

            if (operatorStack.isNotEmpty() && typeStack.size >= 2) {
                val rightType = typeStack.removeAt(typeStack.lastIndex)
                val leftType = typeStack.removeAt(typeStack.lastIndex)
                val operator = operatorStack.removeAt(operatorStack.lastIndex)

                val resultType = when (operator) {
                    "+", "-", "*", "/" -> {
                        if (leftType == rightType && leftType in listOf("%", "!")) {
                            leftType
                        } else {
                            logs.add("Ошибка: несовместимые типы в арифметической операции: $leftType и $rightType.")
                            "unknown"
                        }
                    }
                    "<", ">", "=", "!=", "<=", ">=" -> {
                        if (leftType == rightType && leftType in listOf("%", "!", "$")) {
                            "$" // Результат сравнения — логический тип
                        } else {
                            logs.add("Ошибка: несовместимые типы в операции сравнения: $leftType и $rightType.")
                            "unknown"
                        }
                    }
                    "and", "or" -> {
                        if (leftType == "$" && rightType == "$") {
                            "$"
                        } else {
                            logs.add("Ошибка: логические операции применимы только к булевым типам.")
                            "unknown"
                        }
                    }
                    else -> "unknown"
                }
                typeStack.add(resultType)
            }

            currentIndex++
        }

        return Pair(getType() ?: "unknown", currentIndex)
    }

}
