object SyntaxAnalyzer {

    private val logs = mutableListOf<String>()

    private fun logError(message: String) {
        logs.add("Ошибка: $message")
    }

    private fun logSuccess(message: String) {
        logs.add("Успешно: $message")
    }

    fun analyzeProgram(lexemes: List<Pair<Int, Int>>, reservedWords: List<String>, separators: List<String>, identifiers: List<String>): List<String> {
        logs.clear()

        var index = 0

        if (getLexeme(lexemes[index], reservedWords, separators, identifiers) != "begin") {
            logError("Программа должна начинаться с 'begin'.")
            return logs
        }
        logSuccess("Программа начинается с 'begin'.")
        index++

        while (index < lexemes.size) {
            val lexeme = getLexeme(lexemes[index], reservedWords, separators, identifiers)
            when (lexeme) {
                "dim" -> index = analyzeDeclaration(lexemes, index, identifiers) ?: return logs
                "if" -> index = analyzeConditional(lexemes, index) ?: return logs
                "for" -> index = analyzeFixedCycle(lexemes, index) ?: return logs
                "do" -> index = analyzeConditionalCycle(lexemes, index) ?: return logs
                "end" -> {
                    logSuccess("Программа успешно завершена 'end'.")
                    return logs
                }
                else -> {
                    logError("Неизвестный оператор или структура '$lexeme'.")
                    return logs
                }
            }
            index++
        }

        logError("Программа должна заканчиваться 'end'.")
        return logs
    }

    private fun getLexeme(pair: Pair<Int, Int>, reservedWords: List<String>, separators: List<String>, identifiers: List<String>): String {
        return when (pair.first) {
            1 -> reservedWords[pair.second - 1]
            2 -> separators[pair.second - 1]
            4 -> identifiers[pair.second - 1]
            else -> "Неизвестная лексема"
        }
    }

    private fun analyzeDeclaration(lexemes: List<Pair<Int, Int>>, index: Int, identifiers: List<String>): Int? {
        var currentIndex = index + 1
        if (currentIndex >= lexemes.size || getLexeme(lexemes[currentIndex], listOf(), listOf(), identifiers).isEmpty()) {
            logError("После 'dim' должен идти идентификатор.")
            return null
        }

        while (currentIndex < lexemes.size) {
            val lexeme = getLexeme(lexemes[currentIndex], listOf(), listOf(), identifiers)
            if (lexeme == ",") {
                currentIndex++
                if (currentIndex >= lexemes.size || getLexeme(lexemes[currentIndex], listOf(), listOf(), identifiers).isEmpty()) {
                    logError("После ',' должен идти идентификатор.")
                    return null
                }
            } else if (lexeme in listOf("%", "!", "$")) {
                logSuccess("Описание данных корректно.")
                return currentIndex
            } else {
                break
            }
            currentIndex++
        }

        logError("Описание данных должно заканчиваться типом данных ('%', '!', '$').")
        return null
    }

    private fun analyzeConditional(lexemes: List<Pair<Int, Int>>, index: Int): Int? {
        var currentIndex = index + 1

        if (currentIndex >= lexemes.size || !isValidExpression(lexemes, currentIndex)) {
            logError("После 'if' должно идти выражение.")
            return null
        }

        currentIndex++
        if (currentIndex >= lexemes.size || getLexeme(lexemes[currentIndex], listOf(), listOf(), listOf()) != "then") {
            logError("После выражения в 'if' должен быть 'then'.")
            return null
        }

        currentIndex++
        if (currentIndex >= lexemes.size) {
            logError("После 'then' должен идти оператор.")
            return null
        }

        logSuccess("Условный оператор корректен.")
        return currentIndex
    }

    private fun analyzeFixedCycle(lexemes: List<Pair<Int, Int>>, index: Int): Int? {
        var currentIndex = index + 1
        if (currentIndex >= lexemes.size || getLexeme(lexemes[currentIndex], listOf(), listOf(), listOf()) != "(") {
            logError("После 'for' должна быть '('.")
            return null
        }
        currentIndex++

        var semicolonCount = 0
        while (currentIndex < lexemes.size) {
            val lexeme = getLexeme(lexemes[currentIndex], listOf(), listOf(), listOf())
            if (lexeme == ";") {
                semicolonCount++
            } else if (lexeme == ")") {
                if (semicolonCount == 2) {
                    logSuccess("Фиксированный цикл корректен.")
                    return currentIndex
                } else {
                    logError("В 'for' должно быть 2 ';'.")
                    return null
                }
            }
            currentIndex++
        }

        logError("Фиксированный цикл должен заканчиваться ')'.")
        return null
    }

    private fun analyzeConditionalCycle(lexemes: List<Pair<Int, Int>>, index: Int): Int? {
        var currentIndex = index + 1
        if (currentIndex >= lexemes.size || getLexeme(lexemes[currentIndex], listOf(), listOf(), listOf()) != "while") {
            logError("После 'do' должен быть 'while'.")
            return null
        }

        currentIndex++
        if (currentIndex >= lexemes.size || !isValidExpression(lexemes, currentIndex)) {
            logError("После 'while' должно быть выражение.")
            return null
        }

        logSuccess("Условный цикл корректен.")
        return currentIndex
    }

    private fun isValidExpression(lexemes: List<Pair<Int, Int>>, index: Int): Boolean {
        // Здесь будут правила для проверки выражений
        return true
    }
}
