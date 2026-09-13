package ru.explosive.caremom.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.data.HealthEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 40f

    fun exportChildHistory(child: Child, events: List<HealthEvent>): File? {
        val pdfDocument = PdfDocument()
        val textPaint = TextPaint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        var currentPageNumber = 1
        var currentPage = startNewPage(pdfDocument, currentPageNumber)
        var canvas = currentPage.canvas
        var currentY = margin

        // Заголовок
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = true
        canvas.drawText("Отчет о здоровье: ${child.name}", margin, currentY + 20, textPaint)
        currentY += 50f

        // Инфо о ребенке
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = false
        canvas.drawText("Группа крови: ${child.bloodType ?: "-"}", margin, currentY, textPaint)
        currentY += 20f
        canvas.drawText("Аллергии: ${child.allergies ?: "-"}", margin, currentY, textPaint)
        currentY += 20f
        canvas.drawText("Дата рождения: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(child.birthday))}", margin, currentY, textPaint)
        currentY += 40f

        // Список событий
        textPaint.textSize = 16f
        textPaint.isFakeBoldText = true
        canvas.drawText("История событий:", margin, currentY, textPaint)
        currentY += 30f

        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false

        events.forEach { event ->
            val time = dateFormat.format(Date(event.timestamp))
            val titleText = "$time - ${event.type}: ${event.title}"
            
            if (currentY + 60 > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                currentPage = startNewPage(pdfDocument, currentPageNumber)
                canvas = currentPage.canvas
                currentY = margin
            }

            textPaint.isFakeBoldText = true
            canvas.drawText(titleText, margin, currentY, textPaint)
            currentY += 15f
            textPaint.isFakeBoldText = false

            if (!event.description.isNullOrBlank()) {
                val description = "Комментарий: ${event.description}"
                val staticLayout = StaticLayout.Builder.obtain(description, 0, description.length, textPaint, (pageWidth - 2 * margin - 20).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()

                if (currentY + staticLayout.height > pageHeight - margin) {
                    pdfDocument.finishPage(currentPage)
                    currentPageNumber++
                    currentPage = startNewPage(pdfDocument, currentPageNumber)
                    canvas = currentPage.canvas
                    currentY = margin
                }

                canvas.save()
                canvas.translate(margin + 20, currentY)
                staticLayout.draw(canvas)
                canvas.restore()
                currentY += staticLayout.height + 10f
            }
            
            if (!event.value.isNullOrBlank()) {
                canvas.drawText("   Показатель: ${event.value}", margin, currentY, textPaint)
                currentY += 15f
            }
            currentY += 10f
        }

        pdfDocument.finishPage(currentPage)
        val fileName = "CareMom_${child.name}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun exportFullSummary(childrenData: List<Pair<Child, List<HealthEvent>>>): File? {
        val pdfDocument = PdfDocument()
        val textPaint = TextPaint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        
        var currentPageNumber = 1
        var currentPage = startNewPage(pdfDocument, currentPageNumber)
        var canvas = currentPage.canvas
        var currentY = margin

        textPaint.textSize = 22f
        textPaint.isFakeBoldText = true
        canvas.drawText("Общий отчет CareMom", margin, currentY + 25, textPaint)
        currentY += 60f

        childrenData.forEach { (child, events) ->
            if (currentY + 100 > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                currentPage = startNewPage(pdfDocument, currentPageNumber)
                canvas = currentPage.canvas
                currentY = margin
            }

            textPaint.textSize = 18f
            textPaint.isFakeBoldText = true
            canvas.drawText(child.name, margin, currentY, textPaint)
            currentY += 25f
            
            textPaint.textSize = 12f
            textPaint.isFakeBoldText = false
            canvas.drawText("Всего записей: ${events.size}", margin, currentY, textPaint)
            currentY += 20f
            
            val lastEvent = events.maxByOrNull { it.timestamp }
            if (lastEvent != null) {
                canvas.drawText("Последняя активность: ${dateFormat.format(Date(lastEvent.timestamp))}", margin, currentY, textPaint)
                currentY += 20f
            }
            currentY += 30f
        }

        pdfDocument.finishPage(currentPage)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CareMom_Summary_${System.currentTimeMillis()}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun startNewPage(pdfDocument: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        return pdfDocument.startPage(pageInfo)
    }
}
