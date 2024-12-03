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
    Window(onCloseRequest = ::exitApplication, title = "Лексический и синтаксический анализатор") {
        AppContent()
    }
}

@Composable
fun AppContent() {
    var inputText by remember { mutableStateOf("DIM x (* Это комментарий *)\nx = 42") }
    var analysisResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var numbers by remember { mutableStateOf<List<String>>(emptyList()) }
    var identifiers by remember { mutableStateOf<List<String>>(emptyList()) }
    var logs by remember { mutableStateOf("Ожидание команды...") }

    val separators = LexicalAnalyzer.separators
    val reservedWords = LexicalAnalyzer.reservedWords

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            // Левая колонка с двумя таблицами
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                TableWithHeader(title = "Таблица служебных слов", items = reservedWords, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
                TableWithHeader(title = "Таблица разделителей", items = separators, modifier = Modifier.weight(1f))
            }

            // Правая колонка с двумя таблицами
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                TableWithHeader(title = "Таблица чисел", items = numbers, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
                TableWithHeader(title = "Таблица идентификаторов", items = identifiers, modifier = Modifier.weight(1f))
            }

            // Центральная область для текста и результатов
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
        // Нижняя часть интерфейса
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(80.dp)
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(logs)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    // Очистка логов
                    logs = "Запущен анализ...\n"

                    // Очистка таблиц перед анализом
                    analysisResults = emptyList()
                    numbers = emptyList()
                    identifiers = emptyList()

                    //лексический анализ
                    val lexicalAnalysis = LexicalAnalyzer.analyzeText(inputText)
                    analysisResults = lexicalAnalysis["results"] ?: emptyList()
                    numbers = lexicalAnalysis["numbers"] ?: emptyList()
                    identifiers = lexicalAnalysis["identifiers"] ?: emptyList()
                    logs += "Лексический анализ завершен успешно.\n"

                    //синтаксический анализ
                    val syntaxLogs = SyntaxAnalyzer.analyzeProgram(
                        analysisResults.map {
                            val parts = it.removeSurrounding("(", ")").split(", ")
                            Pair(parts[0].toInt(), parts[1].toInt())
                        },
                        reservedWords,
                        separators,
                        identifiers
                    )

                    //логи синтаксического анализа
                    syntaxLogs.forEach { log ->
                        logs += "$log\n"
                    }
                },
                modifier = Modifier.width(150.dp).height(50.dp)
            ) {
                Text("Анализ")
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
