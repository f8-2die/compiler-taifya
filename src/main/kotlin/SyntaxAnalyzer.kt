class SyntaxAnalyzer(private val tokens: List<Token>) {

    private var currentIndex = 0

    fun analyze(): List<String> {
        val logs = mutableListOf<String>()

        if (tokens.isEmpty()) {
            logs.add("Программа пуста.")
            return logs
        }
        if (tokens.last().value != "end") {
            logs.add("Ошибка: Программа должна заканчиваться 'end'.")
            return logs
        }

        try {
            if (!consume("begin")) {
                throw SyntaxException("Программа должна начинаться с 'begin'.")
            }
            logs.add("Начало программы найдено.")

            while (!check("end")) {
                println("Текущий токен: ${currentToken().value}")

                if (checkType()) {
                    logs.add(parseTypeDeclaration())
                } else if (check("let") || checkIdentifier()) {
                    logs.add(parseAssignment())
                } else if (check("dim")) {
                    logs.add(parseDescription())
                } else if (check("if")) {
                    logs.add(parseIfStatement())
                } else if (check("{")) {
                    logs.add(parseCompoundOperator())
                } else if (check("for")) {
                    logs.add(parseForLoop())
                } else if (check("do")) {
                    logs.add(parseDoWhileLoop())
                } else if (check("input")) {
                    logs.add(parseInput())
                } else if (check("output")) {
                    logs.add(parseOutput())
                } else {
                    throw SyntaxException("Неожиданный токен: ${currentToken().value}.")
                }

                if (check(";")) {
                    consume(";")
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
        println("Проверка consume: ожидается '$expected', текущий токен: '${tokens[currentIndex].value}'")
        if (currentIndex < tokens.size && tokens[currentIndex].value == expected) {
            currentIndex++
            return true
        }
        return false
    }

    private fun check(expected: String): Boolean {
        println("Проверка check: ожидается '$expected', текущий токен: '${tokens[currentIndex].value}'")
        return currentIndex < tokens.size && tokens[currentIndex].value == expected
    }

    private fun checkType(): Boolean {
        println("Проверка checkType: текущий токен: '${tokens[currentIndex].value}'")
        return currentIndex < tokens.size && listOf("%", "!", "$").contains(tokens[currentIndex].value)
    }

    private fun checkIdentifier(): Boolean {
        println("Проверка checkIdentifier: текущий токен: '${tokens[currentIndex].value}'")
        return currentIndex < tokens.size && tokens[currentIndex].type == TokenType.IDENTIFIER
    }

    private fun parseTypeDeclaration(): String {
        val type = currentToken().value
        println("Парсинг типа: $type")
        currentIndex++
        if (!checkIdentifier()) {
            throw SyntaxException("Ожидался идентификатор после типа.")
        }
        val identifier = currentToken().value
        println("Объявлена переменная: $identifier с типом $type")
        currentIndex++
        return "Объявлена переменная: $identifier с типом $type."
    }

    private fun parseAssignment(): String {
        if (check("let")) {
            consume("let")
        }
        if (!checkIdentifier()) {
            throw SyntaxException("Ожидался идентификатор после 'let'.")
        }
        val identifier = currentToken().value
        println("Присваивание переменной: $identifier")
        currentIndex++
        if (!consume("=")) {
            throw SyntaxException("Ожидался оператор '=' после идентификатора.")
        }
        val expression = parseExpression()
        println("Присваивание завершено: $identifier = $expression")
        return "Присваивание: $identifier = $expression"
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

    private fun parseExpression(): String {
        val expression = StringBuilder()

        if (check("not")) {
            consume("not")
            expression.append("not ").append(parseExpression())
        } else if (check("(")) {
            consume("(")
            expression.append("(").append(parseExpression())
            if (!consume(")")) {
                throw SyntaxException("Ожидалась ')'")
            }
            expression.append(")")
        } else if (checkIdentifier()) {
            expression.append(currentToken().value)
            currentIndex++
        } else if (checkNumber()) {
            val number = currentToken().value
            expression.append(number)
            currentIndex++
        }

        else if (check("true") || check("false")) {
            expression.append(currentToken().value)
            currentIndex++
        } else {
            throw SyntaxException("Неверное выражение: ${currentToken().value}")
        }

        while (check("+") || check("-")
            || check("or") || check("*")
            || check("/") || check("and")
            || check("<") || check(">")
            || check("=")) {
            expression.append(" ").append(currentToken().value)
            currentIndex++
            expression.append(" ").append(parseExpression())
        }
        return expression.toString()
    }

    private fun checkNumber(): Boolean {
        val currentTokenValue = currentToken().value
        return currentIndex < tokens.size &&
                (currentToken().type == TokenType.NUMBER ||
                        isNumberInBase(currentTokenValue))
    }

    private fun isNumberInBase(number: String): Boolean {
        return number.matches(Regex("[01]+b")) ||
                number.matches(Regex("[0-7]+o")) ||
                number.matches(Regex("[0-9A-Fa-f]+h")) ||
                number.matches(Regex("\\d+(\\.\\d+)?"))
    }

    private fun parseIfStatement(): String {
        if (!consume("if")) {
            throw SyntaxException("Ожидалось 'if'.")
        }
        val condition = parseExpression()
        if (!consume("then")) {
            throw SyntaxException("Ожидалось 'then'.")
        }
        if (!condition.contains("true") && !condition.contains("false") && !condition.contains("not") &&
            !condition.contains("or") && !condition.contains("and") && !condition.contains("<") &&
            !condition.contains(">") && !condition.contains("=")
        ) {
            throw SyntaxException("Условие должно быть булевым.")
        }
        val thenBranch = parseOperator()
        consume(";")

        var elseBranch = ""
        if (consume("else")) {
            elseBranch = parseOperator()
            consume(";")
        }
        if (!consume("end_else")) {
            throw SyntaxException("Ожидалось 'end_else'.")
        }
        return "Условный оператор: if $condition then $thenBranch else $elseBranch"
    }

    private fun parseOperator(): String {
        val result = when {
            check("let") || checkIdentifier() -> parseAssignment()
            check("if") -> parseIfStatement()
            check("{") -> parseCompoundOperator()
            check("for") -> parseForLoop()
            check("do") -> parseDoWhileLoop()
            check("input") -> parseInput()
            check("output") -> parseOutput()
            else -> throw SyntaxException("Ожидался оператор.")
        }
        consume(";")
        return result
    }

    private fun parseInput(): String {
        if (!consume("input")) {
            throw SyntaxException("Ожидалось 'input'.")
        }
        if (!consume("(")) {
            throw SyntaxException("Ожидалась '('.")
        }
        val identifiers = mutableListOf<String>()
        do {
            if (!checkIdentifier()) {
                throw SyntaxException("Ожидался идентификатор в операторе ввода.")
            }
            identifiers.add(currentToken().value)
            currentIndex++
        } while (consume(","))
        if (!consume(")")) {
            throw SyntaxException("Ожидалась ')'.")
        }
        return "Оператор ввода: ${identifiers.joinToString(", ")}"
    }

    private fun parseOutput(): String {
        if (!consume("output")) {
            throw SyntaxException("Ожидалось 'output'.")
        }
        if (!consume("(")) {
            throw SyntaxException("Ожидалась '('.")
        }
        val expressions = mutableListOf<String>()
        do {
            expressions.add(parseExpression())
        } while (consume(","))
        if (!consume(")")) {
            throw SyntaxException("Ожидалась ')'.")
        }
        return "Оператор вывода: ${expressions.joinToString(", ")}"
    }

    private fun parseForLoop(): String {
        if (!consume("for")) {
            throw SyntaxException("Ожидалось 'for'.")
        }
        println("Обработка цикла for")

        if (!consume("(")) {
            throw SyntaxException("Ожидалась '('.")
        }

        val initExpression = if (checkIdentifier() || check("let")) parseAssignment() else ""
        println("Инициализация цикла: $initExpression") // Лог
        if (!consume(";")) {
            throw SyntaxException("Ожидалась ';' после инициализации цикла.")
        }

        val conditionExpression = if (checkIdentifier() || checkNumber() || check("(")) parseExpression() else ""
        println("Условие цикла: $conditionExpression") // Лог
        if (!consume(";")) {
            throw SyntaxException("Ожидалась ';' после условия цикла.")
        }

        val updateExpression = if (checkIdentifier() || check("let")) parseAssignment() else ""
        println("Обновление переменной цикла: $updateExpression") // Лог
        if (!consume(")")) {
            throw SyntaxException("Ожидалась ')'.")
        }

        if (!check("{")) {
            throw SyntaxException("Ожидалось '{' после заголовка цикла.")
        }
        val body = parseCompoundOperator()
        println("Тело цикла: $body")

        return "Цикл for: ($initExpression; $conditionExpression; $updateExpression) $body"
    }

    private fun parseCompoundOperator(): String {
        if (!consume("{")) {
            throw SyntaxException("Ожидалась '{'.")
        }
        println("Обработка составного оператора") // Лог

        val operators = mutableListOf<String>()
        while (!check("}")) {
            when {
                checkIdentifier() || check("let") -> operators.add(parseAssignment())
                check("if") -> operators.add(parseIfStatement())
                check("for") -> operators.add(parseForLoop())
                check("do") -> operators.add(parseDoWhileLoop())
                check("input") -> operators.add(parseInput())
                check("output") -> operators.add(parseOutput())
                else -> throw SyntaxException("Ожидался оператор в теле составного оператора, но найден: ${currentToken().value}")
            }
            if (!check(";") && !check("}")) {
                throw SyntaxException("Ожидался ';' или '}' после оператора.")
            }
            consume(";")
        }

        if (!consume("}")) {
            throw SyntaxException("Ожидалась '}' в конце составного оператора.")
        }
        return "Составной оператор: ${operators.joinToString("; ")}"
    }

    private fun parseDoWhileLoop(): String {
        if (!consume("do")) {
            throw SyntaxException("Ожидалось 'do'.")
        }
        println("Начало цикла do while")

        val body = parseOperator()
        println("Тело цикла: $body")

        if (!consume("while")) {
            throw SyntaxException("Ожидалось 'while' после тела цикла.")
        }

        val condition = parseExpression()
        println("Условие цикла: $condition")

        if (!consume("loop")) {
            throw SyntaxException("Ожидалось 'loop' в конце конструкции do while.")
        }
        println("Конструкция do while завершена.")

        return "Цикл do while: тело {$body}, условие {$condition}"
    }

}

class SyntaxException(message: String) : Exception(message)

data class Token(val value: String, val type: TokenType)

enum class TokenType {
    IDENTIFIER, KEYWORD, NUMBER, SYMBOL
}