package org.example.domain.service

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class DocumentService {

    /**
     * Читает .docx поток и превращает его в Markdown строку.
     */
    fun convertDocxToMarkdown(inputStream: InputStream): String {
        // XWPFDocument - класс для работы с .docx
        val document = XWPFDocument(inputStream)
        val sb = StringBuilder()

        // Итерируемся по параграфам документа
        for (paragraph in document.paragraphs) {
            val text = paragraph.text.trim()

            if (text.isBlank()) {
                sb.append("\n") // Сохраняем пустые строки
                continue
            }

            // Пытаемся определить стиль (Заголовок 1, 2 и т.д.)
            val style = paragraph.style

            // В Word стили могут называться "Heading1", "Heading 1" или локализованно.
            // Проверяем по ID стиля или началу названия.
            val prefix = when {
                style != null && (style.equals("Heading1", ignoreCase = true) || style.startsWith("1")) -> "# "
                style != null && (style.equals("Heading2", ignoreCase = true) || style.startsWith("2")) -> "## "
                style != null && (style.equals("Heading3", ignoreCase = true) || style.startsWith("3")) -> "### "

                // Простая эвристика: если текст короткий, жирный и без точки в конце - возможно это заголовок
                paragraph.runs.firstOrNull()?.isBold == true && text.length < 50 && !text.endsWith(".") -> "### "

                else -> ""
            }

            // Добавляем текст
            sb.append(prefix).append(text).append("\n\n")
        }

        return sb.toString().trim()
    }
}