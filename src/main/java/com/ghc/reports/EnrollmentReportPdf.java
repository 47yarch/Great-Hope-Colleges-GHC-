package com.ghc.reports;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

public final class EnrollmentReportPdf {

    private EnrollmentReportPdf() {
    }

    public record EnrollmentRow(
            String id,
            String studentNo,
            String studentName,
            String course,
            String schoolYear,
            String semester,
            String grade,
            String remarks) {
    }

    private static final PDColor GREEN =
            new PDColor(new float[]{0.11f, 0.29f, 0.20f}, PDDeviceRGB.INSTANCE);

    private static final PDColor GOLD =
            new PDColor(new float[]{0.90f, 0.75f, 0.25f}, PDDeviceRGB.INSTANCE);

    private static final PDColor BEIGE =
            new PDColor(new float[]{0.96f, 0.95f, 0.91f}, PDDeviceRGB.INSTANCE);

    private static final PDColor BLACK =
            new PDColor(new float[]{0.12f, 0.12f, 0.12f}, PDDeviceRGB.INSTANCE);

    private static final PDColor WHITE =
            new PDColor(new float[]{1f, 1f, 1f}, PDDeviceRGB.INSTANCE);

    public static void generate(
            Path output,
            List<EnrollmentRow> rows) throws IOException {

        try (PDDocument document = new PDDocument()) {

            // Landscape A4 page. PDFBox 2.x has no PDRectangle.rotate().
            PDRectangle pageSize = new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth());

            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float margin = 28;
            float contentWidth = pageWidth - margin * 2;

            try (PDPageContentStream cs =
                         new PDPageContentStream(document, page)) {

                // Green report banner.
                cs.setNonStrokingColor(GREEN);
                cs.addRect(0, pageHeight - 72, pageWidth, 72);
                cs.fill();

                drawText(cs, PDType1Font.HELVETICA_BOLD, 18,
                        WHITE, margin, pageHeight - 31,
                        "GREAT HOPE COLLEGES (GHC)");

                drawText(cs, PDType1Font.HELVETICA, 10,
                        WHITE, margin, pageHeight - 49,
                        "Student Enrollment & Academic Term Report");

                String date = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy"));

                drawText(cs, PDType1Font.HELVETICA, 9,
                        WHITE, pageWidth - 155, pageHeight - 30,
                        "Date: " + date);

                drawText(cs, PDType1Font.HELVETICA, 8,
                        WHITE, pageWidth - 155, pageHeight - 45,
                        "System: Academic Management");

                // Summary cards.
                float cardY = pageHeight - 112;
                float cardGap = 18;
                float cardWidth = (contentWidth - cardGap * 2) / 3;
                int total = rows.size();
                int passed = 0;
                double gradeTotal = 0;
                int graded = 0;

                for (EnrollmentRow row : rows) {
                    String grade = safe(row.grade());

                    try {
                        double value = Double.parseDouble(grade);
                        gradeTotal += value;
                        graded++;

                        if (value >= 50) {
                            passed++;
                        }
                    } catch (NumberFormatException ignored) {
                        // Pending or ungraded enrollment.
                    }
                }

                String average = graded == 0
                        ? "N/A"
                        : String.format("%.2f / 100", gradeTotal / graded);

                String status = total == 0
                        ? "No Records"
                        : String.format("%.0f%% Passed",
                                passed * 100.0 / total);

                drawCard(cs, margin, cardY, cardWidth, 38,
                        "TOTAL ENROLLMENTS", total + " Records");

                drawCard(cs, margin + cardWidth + cardGap,
                        cardY, cardWidth, 38,
                        "AVERAGE GRADE", average);

                drawCard(cs, margin + (cardWidth + cardGap) * 2,
                        cardY, cardWidth, 38,
                        "ACADEMIC STATUS", status);

                // Report table.
                float tableTop = cardY - 18;
                float rowHeight = 23;
                float headerHeight = 24;

                float[] widths = {
                    contentWidth * 0.06f,
                    contentWidth * 0.12f,
                    contentWidth * 0.19f,
                    contentWidth * 0.19f,
                    contentWidth * 0.12f,
                    contentWidth * 0.12f,
                    contentWidth * 0.08f,
                    contentWidth * 0.12f
                };

                String[] headers = {
                    "ID", "Student No.", "Student Name", "Course",
                    "School Year", "Semester", "Grade", "Remarks"
                };

                drawTableHeader(cs, margin, tableTop,
                        widths, headers, headerHeight);

                float y = tableTop - headerHeight;

                for (int i = 0; i < rows.size(); i++) {
                    EnrollmentRow row = rows.get(i);

                    if (y - rowHeight < 65) {
                        // Keep this report to one page by stopping before
                        // the footer overlaps the data.
                        drawText(cs, PDType1Font.HELVETICA_OBLIQUE, 8,
                                BLACK, margin, 55,
                                "... additional records omitted; export a smaller search result.");
                        break;
                    }

                    if (i % 2 == 1) {
                        cs.setNonStrokingColor(BEIGE);
                        cs.addRect(margin, y - rowHeight,
                                contentWidth, rowHeight);
                        cs.fill();
                    }

                    String[] values = {
                        row.id(), row.studentNo(), row.studentName(),
                        row.course(), row.schoolYear(), row.semester(),
                        row.grade(), row.remarks()
                    };

                    float x = margin;

                    for (int col = 0; col < values.length; col++) {
                        cs.setStrokingColor(
                                new PDColor(
                                        new float[]{0.82f, 0.80f, 0.74f},
                                        PDDeviceRGB.INSTANCE));
                        cs.addRect(x, y - rowHeight,
                                widths[col], rowHeight);
                        cs.stroke();

                        String value = truncate(safe(values[col]), 25);

                        drawText(cs, PDType1Font.HELVETICA, 8,
                                BLACK, x + 5, y - 14, value);

                        x += widths[col];
                    }

                    y -= rowHeight;
                }

                // Approval area and footer.
                float footerY = 40;

                cs.setStrokingColor(GREEN);
                cs.setLineWidth(1);
                cs.moveTo(pageWidth - margin - 190, footerY + 22);
                cs.lineTo(pageWidth - margin, footerY + 22);
                cs.stroke();

                drawText(cs, PDType1Font.HELVETICA, 8,
                        BLACK, pageWidth - margin - 135,
                        footerY + 10, "Approved / Verified By");

                drawText(cs, PDType1Font.HELVETICA, 8,
                        GOLD, pageWidth - margin - 135,
                        footerY - 1, "Office of the Registrar");

                cs.setStrokingColor(
                        new PDColor(
                                new float[]{0.82f, 0.80f, 0.74f},
                                PDDeviceRGB.INSTANCE));
                cs.moveTo(margin, 28);
                cs.lineTo(pageWidth - margin, 28);
                cs.stroke();

                drawText(cs, PDType1Font.HELVETICA, 7,
                        BLACK, margin, 16,
                        "Great Hope Colleges - Student Enrollment & Academic Term System");

                drawText(cs, PDType1Font.HELVETICA, 7,
                        BLACK, pageWidth - margin - 45, 16,
                        "Page 1 of 1");
            }

            document.save(output.toFile());
        }
    }

    private static void drawCard(
            PDPageContentStream cs,
            float x,
            float y,
            float width,
            float height,
            String heading,
            String value) throws IOException {

        cs.setNonStrokingColor(BEIGE);
        cs.addRect(x, y, width, height);
        cs.fill();

        cs.setNonStrokingColor(GREEN);
        cs.addRect(x, y, 5, height);
        cs.fill();

        drawText(cs, PDType1Font.HELVETICA, 7,
                BLACK, x + 12, y + height - 12, heading);

        drawText(cs, PDType1Font.HELVETICA_BOLD, 11,
                GREEN, x + 12, y + 10, value);
    }

    private static void drawTableHeader(
            PDPageContentStream cs,
            float x,
            float top,
            float[] widths,
            String[] headers,
            float height) throws IOException {

        float currentX = x;

        for (int i = 0; i < headers.length; i++) {
            cs.setNonStrokingColor(GREEN);
            cs.addRect(currentX, top - height, widths[i], height);
            cs.fill();

            cs.setStrokingColor(WHITE);
            cs.addRect(currentX, top - height, widths[i], height);
            cs.stroke();

            drawText(cs, PDType1Font.HELVETICA_BOLD, 8,
                    WHITE, currentX + 5, top - 15, headers[i]);

            currentX += widths[i];
        }
    }

    private static void drawText(
            PDPageContentStream cs,
            org.apache.pdfbox.pdmodel.font.PDFont font,
            float size,
            PDColor color,
            float x,
            float y,
            String text) throws IOException {

        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(text));
        cs.endText();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }

        // Standard PDF fonts support WinAnsi, so replace unsupported
        // characters and normalize common display symbols.
        return value
                .replace("–", "-")
                .replace("—", "-")
                .replace("•", "-")
                .replace("₱", "PHP ")
                .replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}