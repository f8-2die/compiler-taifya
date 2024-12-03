object LexicalAnalyzer {
    val reservedWords: List<String> = listOf(
        "begin", "end", "dim", "let", "if", "then", "else",
        "end_else", "for", "do", "input", "output", "while",
        "%", "!", "$", "loop"
    )

    val separators: List<String> = listOf(
        "<>", "=", "<", "<=", ">", ">=", "+", "-", "*", "/", ";", ",",
        "}", "{", "(", ")", "or", "and", "not"
    )

    private val results = mutableListOf<String>()
    private val numbers = mutableListOf<String>()
    private val identifiers = mutableListOf<String>()

    private fun clearTables() {
        results.clear()
        numbers.clear()
        identifiers.clear()
    }

    private var buffer = StringBuilder()
    private fun nill() = buffer.clear()
    private fun add(char: Char) = buffer.append(char)

    private fun gc(input: String, position: Int): Char? {
        return if (position < input.length) input[position] else null
    }

    private fun look(table: List<String>): Int {
        return table.indexOf(buffer.toString()) + 1
    }

    private fun put(table: MutableList<String>): Int {
        val lexeme = buffer.toString()
        if (!table.contains(lexeme)) {
            table.add(lexeme)
        }
        return table.indexOf(lexeme) + 1
    }

    private fun out(tableNumber: Int, position: Int) {
        results.add("($tableNumber, $position)")
    }

    private fun toBinaryWithFraction(decimal: Double): String {
        val integerPart = decimal.toInt()
        val fractionalPart = decimal - integerPart

        val binaryIntegerPart = integerPart.toString(2)
        val binaryFractionPart = StringBuilder()

        var fraction = fractionalPart
        var iterations = 0
        while (fraction > 0 && iterations < 10) {
            fraction *= 2
            val bit = fraction.toInt()
            binaryFractionPart.append(bit)
            fraction -= bit
            iterations++
        }

        return if (binaryFractionPart.isNotEmpty()) {
            "$binaryIntegerPart.${binaryFractionPart.toString()}"
        } else {
            binaryIntegerPart
        }
    }

    private fun isValidNumber(input: String, base: Int): Boolean {
        return when (base) {
            2 -> input.matches(Regex("[01]+(\\.[01]+)?b"))
            8 -> input.matches(Regex("[0-7]+(\\.[0-7]+)?o"))
            16 -> input.matches(Regex("[0-9A-Fa-f]+(\\.[0-9A-Fa-f]+)?h"))
            10 -> input.matches(Regex("\\d+(\\.\\d+)?"))
            else -> false
        }
    }

    fun analyzeText(inputText: String): Map<String, List<String>> {
        clearTables()
        nill()

        var position = 0
        var state = "H"
        var insideComment = false

        while (position < inputText.length) {
            val currentChar = gc(inputText, position)

            if (insideComment) {
                if (currentChar == '*' && position + 1 < inputText.length && inputText[position + 1] == ')') {
                    insideComment = false
                    position++
                }
                position++
                continue
            }

            when (state) {
                "H" -> when {
                    currentChar == null -> break
                    currentChar.isWhitespace() -> { /* Пропускаем пробелы */ }
                    currentChar == '(' && position + 1 < inputText.length && inputText[position + 1] == '*' -> {
                        insideComment = true
                        position++
                    }
                    separators.any { it.startsWith(currentChar.toString()) } -> {
                        val twoCharSeparator = if (position + 1 < inputText.length) {
                            separators.find { it == inputText.substring(position, position + 2) }
                        } else null

                        if (twoCharSeparator != null) {
                            val positionInTable = separators.indexOf(twoCharSeparator) + 1
                            out(2, positionInTable)
                            position++
                        } else {
                            val positionInTable = separators.indexOf(currentChar.toString()) + 1
                            if (positionInTable > 0) {
                                out(2, positionInTable)
                            } else {
                                results.add("Ошибка на символе: $currentChar")
                            }
                        }
                    }
                    currentChar in listOf('%', '!', '$') -> {
                        val positionInTable = reservedWords.indexOf(currentChar.toString()) + 1
                        if (positionInTable > 0) {
                            out(1, positionInTable)
                        } else {
                            results.add("Ошибка на символе: $currentChar")
                        }
                    }
                    currentChar.isLetter() || currentChar == '_' -> {
                        state = "I"
                        nill()
                        add(currentChar)
                    }
                    currentChar.isDigit() -> {
                        state = "NUM"
                        nill()
                        add(currentChar)
                    }
                    else -> {
                        results.add("Ошибка на символе: $currentChar")
                    }
                }

                "I" -> when {
                    currentChar?.isLetterOrDigit() == true || currentChar == '_' -> add(currentChar)
                    else -> {
                        val z = look(reservedWords)
                        if (z != 0) out(1, z) else out(4, put(identifiers))
                        state = "H"
                        position--
                    }
                }

                "NUM" -> when {
                    currentChar?.isDigit() == true || currentChar == '.' || currentChar in listOf('b', 'o', 'h') -> currentChar?.let {
                        add(
                            it
                        )
                    }
                    else -> {
                        val number = buffer.toString()
                        val (isValid, base) = when {
                            isValidNumber(number, 2) -> true to 2
                            isValidNumber(number, 8) -> true to 8
                            isValidNumber(number, 16) -> true to 16
                            isValidNumber(number, 10) -> true to 10
                            else -> false to 10
                        }

                        if (isValid) {
                            val decimal = when (base) {
                                2 -> number.dropLast(1).replace(".", "").toInt(2).toDouble()
                                8 -> number.dropLast(1).replace(".", "").toInt(8).toDouble()
                                16 -> number.dropLast(1).replace(".", "").toInt(16).toDouble()
                                else -> number.toDouble()
                            }

                            val binary = toBinaryWithFraction(decimal)
                            val representation = "число $decimal $base-ричное в двоичном виде выглядит как $binary"
                            if (!numbers.contains(representation)) {
                                numbers.add(representation)
                            }
                            out(3, numbers.indexOf(representation) + 1)
                        } else {
                            results.add("Ошибка в числе: $number")
                        }

                        state = "H"
                        position--
                    }
                }
            }
            position++
        }

        return mapOf(
            "results" to results,
            "numbers" to numbers,
            "identifiers" to identifiers
        )
    }

}