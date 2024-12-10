import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Простой компилятор. Ларкин РПИС-11") {
        AppContent()
    }
}

@Composable
fun AppContent() {
    var inputText by remember { mutableStateOf("DIM x (* Это комментарий *)\nx = 42") }
    var analysisResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var numbers by remember { mutableStateOf<List<String>>(emptyList()) }
    var identifiers by remember { mutableStateOf<List<String>>(emptyList()) }
    var logs by remember { mutableStateOf(listOf("Ожидание команды...")) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }

    val separators = LexicalAnalyzer.separators
    val reservedWords = LexicalAnalyzer.reservedWords

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            // Левая колонка с таблицами
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                TableWithHeader(title = "Таблица служебных слов", items = reservedWords, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
                TableWithHeader(title = "Таблица разделителей", items = separators, modifier = Modifier.weight(1f))
            }

            // Правая колонка с таблицами
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                TableWithHeader(title = "Таблица чисел", items = numbers, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
                TableWithHeader(title = "Таблица идентификаторов", items = identifiers, modifier = Modifier.weight(1f))
            }

            // Центральная область
            Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
                BoxWithHeader(title = "Исходный текст", modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (inputText.isEmpty()) {
                                    Text("Введите текст здесь...", color = Color.Gray)
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                BoxWithHeader(title = "Результаты анализа", modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(analysisResults) { result ->
                            Text(result)
                        }
                    }
                }
            }
        }

        // Логи
        BoxWithHeader(title = "Логи", modifier = Modifier.height(150.dp).fillMaxWidth()) {
            val logListState = rememberLazyListState()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp),
                    state = logListState
                ) {
                    items(logs) { log ->
                        Text(log, modifier = Modifier.padding(4.dp), color = Color.Black)
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(logListState),
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    logs = listOf("Запущен анализ...")

                    try {
                        val lexicalAnalysis = LexicalAnalyzer.analyzeText(inputText)
                        analysisResults = lexicalAnalysis["results"]?.toList() ?: emptyList()
                        numbers = lexicalAnalysis["numbers"]?.toList() ?: emptyList()
                        identifiers = lexicalAnalysis["identifiers"]?.toList() ?: emptyList()
                        logs = logs + "Лексический анализ завершен успешно."

                        val tokens = analysisResults.map { result ->
                            val parts = result.removeSurrounding("(", ")").split(", ")
                            val tableNumber = parts[0].toInt()
                            val index = parts[1].toInt()

                            when (tableNumber) {
                                1 -> Token(LexicalAnalyzer.reservedWords[index - 1], TokenType.KEYWORD)
                                2 -> Token(LexicalAnalyzer.separators[index - 1], TokenType.SYMBOL)
                                3 -> Token(LexicalAnalyzer.numbers[index - 1], TokenType.NUMBER)
                                4 -> Token(LexicalAnalyzer.identifiers[index - 1], TokenType.IDENTIFIER)
                                else -> throw IllegalArgumentException("Неизвестная таблица: $tableNumber")
                            }
                        }

                        val syntaxAnalyzer = SyntaxAnalyzer(tokens)
                        logs = logs + syntaxAnalyzer.analyze()

                        val semanticAnalyzer = SemanticAnalyzer(tokens)
                        val semanticLogs = semanticAnalyzer.analyze()
                        logs = logs + semanticLogs

                        val errors = logs.filter { it.contains("Ошибка") }
                        if (errors.isNotEmpty()) {
                            errorDialogMessage = errors.joinToString("\n")
                            showErrorDialog = true
                        }
                    } catch (e: Exception) {
                        logs = logs + "Ошибка анализа: ${e.message}"
                        errorDialogMessage = "Ошибка анализа: ${e.message}"
                        showErrorDialog = true
                    }
                },
                modifier = Modifier.width(150.dp).height(50.dp)
            ) {
                Text("Анализ")
            }

        }
    }

    if (showErrorDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showErrorDialog = false }) {
            Box(
                modifier = Modifier
                    .width(400.dp) // Установите фиксированную ширину
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Обнаружены ошибки",
                        color = Color.Red,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDialogMessage,
                        color = Color.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 20.sp, // Регулирует межстрочный интервал
                        softWrap = true, // Включает перенос текста
                        maxLines = Int.MAX_VALUE // Убирает ограничение строк
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showErrorDialog = false }) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }

}

@Composable
fun TableWithHeader(title: String, items: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F8F8))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Color.Black,
            fontSize = 16.sp
        )
        ScrollableTable(items)
    }
}

@Composable
fun ScrollableTable(items: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .fillMaxHeight()
            .background(Color.White)
    ) {
        Row {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp),
                state = listState
            ) {
                items(items.size) { index ->
                    Text(
                        text = "${index + 1}: ${items[index]}",
                        modifier = Modifier.padding(4.dp),
                        color = Color.Black
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(listState)
            )
        }
    }
}

@Composable
fun BoxWithHeader(title: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F8F8))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Color.Black,
            fontSize = 16.sp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(8.dp),
            content = content
        )
    }
}