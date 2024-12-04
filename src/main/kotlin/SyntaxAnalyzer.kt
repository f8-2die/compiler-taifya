class SyntaxAnalyzer(private val tokens: List<Token>) {

    private var currentIndex = 0

    fun analyze(): List<String> {
        val logs = mutableListOf<String>()

        if (tokens.isEmpty()) {
            logs.add("Программа пуста.")
            return logs
        }

        try {
            if (!consume("begin")) {
                throw SyntaxException("Программа должна начинаться с 'begin'.")
            }
            logs.add("Начало программы найдено.")

            while (!check("end")) {
                if (checkType()) {
                    logs.add(parseTypeDeclaration())
                } else if (checkIdentifier()) {
                    logs.add(parseAssignment())
                } else if (check("dim")) {
                    logs.add(parseDescription())
                } else {
                    throw SyntaxException("Неожиданный токен: ${currentToken().value}.")
                }
            }

            if (!consume("end")) {
                throw SyntaxException("Программа должна заканчиваться 'end'.")
            }
            logs.add("Конец программы найден. Синтаксический анализ успешно завершен.")

        } catch (e: SyntaxException) {
            logs.add("Ошибка: ${e.message}")
        }
        return logs
    }

    private fun currentToken(): Token = tokens[currentIndex]

    private fun consume(expected: String): Boolean {
        if (currentIndex < tokens.size && tokens[currentIndex].value == expected) {
            currentIndex++
            return true
        }
        return false
    }

    private fun check(expected: String): Boolean =
        currentIndex < tokens.size && tokens[currentIndex].value == expected

    private fun checkType(): Boolean =
        currentIndex < tokens.size && listOf("%", "!", "$").contains(tokens[currentIndex].value)

    private fun checkIdentifier(): Boolean =
        currentIndex < tokens.size && tokens[currentIndex].type == TokenType.IDENTIFIER

    private fun parseTypeDeclaration(): String {
        val type = currentToken().value
        currentIndex++
        if (!checkIdentifier()) {
            throw SyntaxException("Ожидался идентификатор после типа.")
        }
        val identifier = currentToken().value
        currentIndex++
        return "Объявлена переменная: $identifier с типом $type."
    }

    private fun parseAssignment(): String {
        val identifier = currentToken().value
        currentIndex++
        if (!consume(":=")) {
            throw SyntaxException("Ожидался оператор присваивания ':=' после идентификатора.")
        }
        if (!checkIdentifier()) {
            throw SyntaxException("Ожидался идентификатор или значение после ':='.")
        }
        val value = currentToken().value
        currentIndex++
        return "Присваивание: $identifier := $value."
    }

    private fun parseDescription(): String {
        if (!consume("dim")) {
            throw SyntaxException("Ожидалось ключевое слово 'dim'.")
        }

        val identifiers = mutableListOf<String>()
        do {
            if (!checkIdentifier()) {
                throw SyntaxException("Ожидался идентификатор после 'dim'.")
            }
            identifiers.add(currentToken().value)
            currentIndex++
        } while (consume(","))

        if (!checkType()) {
            throw SyntaxException("Ожидался тип данных (% | ! | $) после списка идентификаторов.")
        }
        val type = currentToken().value
        currentIndex++

        return "Объявлены переменные: ${identifiers.joinToString(", ")} с типом $type."
    }
}

class SyntaxException(message: String) : Exception(message)

data class Token(val value: String, val type: TokenType)

enum class TokenType {
    IDENTIFIER, KEYWORD, NUMBER, SYMBOL
}
