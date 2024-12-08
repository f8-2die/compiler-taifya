object LexicalAnalyzer {
    val reservedWords: List<String> = listOf(
        "begin", "end", "dim", "let", "if", "then", "else",
        "end_else", "for", "do", "input", "output", "while",
        "%", "!", "$", "loop", "true", "false"
    )



    val separators: List<String> = listOf(
        "<>", "=", "<", "<=", ">", ">=", "+", "-", "*", "/", ";", ",",
        "}", "{", "(", ")", "or", "and", "not"
    )

    private val results = mutableListOf<String>()
    val numbers = mutableListOf<String>()
    val identifiers = mutableListOf<String>()

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
                    separators.any { inputText.substring(position).startsWith(it) } -> {
                        val separator = separators.find { inputText.substring(position).startsWith(it) }

                        if (separator != null) {
                            val positionInTable = separators.indexOf(separator) + 1
                            out(2, positionInTable)
                            position += separator.length - 1 // Сдвигаем позицию на длину разделителя
                        } else {
                            results.add("Ошибка на символе: $currentChar")
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
                        val bufferContent = buffer.toString()
                        val reservedIndex = look(reservedWords)
                        val separatorIndex = look(separators)

                        if (reservedIndex != 0) {
                            out(1, reservedIndex)
                        } else if (separatorIndex == 0) {
                            val pos = put(identifiers)
                            println("Идентификатор добавлен в таблицу: ${identifiers[pos - 1]}")
                            out(4, pos)
                        } else {
                            results.add("Ошибка: нераспознанное слово $bufferContent")
                        }
                        state = "H"
                        position--
                    }
                }

                "NUM" -> when {
                    currentChar?.isDigit() == true -> add(currentChar)
                    currentChar == '.' -> {
                        if (buffer.contains(".")) {
                            results.add("Ошибка: некорректное число $buffer$currentChar.")
                            state = "H"
                            position--
                        } else {
                            add(currentChar) // Добавляем точку, если её ещё не было
                        }
                    }
                    else -> {
                        val number = buffer.toString()
                        val isInteger = number.matches(Regex("\\d+"))
                        val isReal = number.matches(Regex("\\d+\\.\\d+")) // Числа с точкой

                        if (isInteger) {
                            val representation = "число $number (%) 10-ричное в двоичном виде выглядит как ${number.toInt().toString(2)}"
                            if (!numbers.contains(representation)) {
                                numbers.add(representation)
                            }
                            out(3, numbers.indexOf(representation) + 1)
                        } else if (isReal) {
                            val decimal = number.toDouble()
                            val binary = toBinaryWithFraction(decimal)
                            val representation = "число $decimal (!) 10-ричное в двоичном виде выглядит как $binary"
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