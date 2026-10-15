package com.ghc.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ReportCardPdf {

    private static final Color GREEN = new Color(35, 100, 62);
    private static final Color DARK_GREEN = new Color(23, 73, 44);
    private static final Color YELLOW = new Color(239, 195, 55);
    private static final Color BEIGE = new Color(246, 240, 222);
    private static final Color ROW_COLOR = new Color(232, 235, 226);
    private static final Color BLACK = Color.BLACK;

    private ReportCardPdf() {}

    public record CourseGrade(
            String code,
            String title,
            String grade,
            int units
    ) {}

    public static void generate(
            Path output,
            String collegeName,
            String studentName,
            String studentNumber,
            String program,
            String yearLevel,
            String schoolYear,
            String semester,
            List<CourseGrade> courses
    ) throws IOException {

        if (output == null || courses == null) {
            throw new IllegalArgumentException(
                    "Output path and course list are required.");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float margin = 42;
            float usableWidth = pageWidth - 2 * margin;

            try (PDPageContentStream cs =
                         new PDPageContentStream(document, page)) {

                // Page background
                cs.setNonStrokingColor(BEIGE);
                cs.addRect(0, 0, pageWidth,
                        page.getMediaBox().getHeight());
                cs.fill();

                // GHC header
                cs.setNonStrokingColor(DARK_GREEN);
                cs.addRect(0, 700, pageWidth, 142);
                cs.fill();

                cs.setNonStrokingColor(YELLOW);
                cs.addRect(margin, 697, usableWidth, 3);
                cs.fill();

                text(cs, PDType1Font.HELVETICA_BOLD, 23,
                        Color.WHITE, margin, 793,
                        safe(collegeName).toUpperCase());

                text(cs, PDType1Font.HELVETICA, 11,
                        Color.WHITE, margin, 771,
                        "GREAT HOPE COLLEGES");

                text(cs, PDType1Font.HELVETICA_BOLD, 25,
                        GREEN, margin, 654, "REPORT CARD");

                cs.setStrokingColor(GREEN);
                cs.setLineWidth(2);
                cs.moveTo(margin, 641);
                cs.lineTo(pageWidth - margin, 641);
                cs.stroke();

                // Student information
                float leftX = margin;
                float rightX = pageWidth / 2 + 10;
                float y = 610;

                info(cs, leftX, y, "STUDENT NAME", studentName);
                info(cs, rightX, y, "STUDENT NUMBER", studentNumber);

                info(cs, leftX, y - 48, "PROGRAM / MAJOR", program);
                info(cs, rightX, y - 48, "YEAR LEVEL", yearLevel);

                info(cs, leftX, y - 96, "SCHOOL YEAR", schoolYear);
                info(cs, rightX, y - 96, "SEMESTER", semester);

                // Course table
                float tableTop = 485;
                float headerHeight = 31;
                float rowHeight = 27;
                float codeWidth = 72;
                float gradeWidth = 65;
                float unitsWidth = 42;
                float titleWidth =
                        usableWidth - codeWidth - gradeWidth - unitsWidth;

                cs.setNonStrokingColor(DARK_GREEN);
                cs.addRect(margin, tableTop - headerHeight,
                        usableWidth, headerHeight);
                cs.fill();

                text(cs, PDType1Font.HELVETICA_BOLD, 10,
                        Color.WHITE, margin + 8, tableTop - 20, "CODE");

                text(cs, PDType1Font.HELVETICA_BOLD, 10,
                        Color.WHITE, margin + codeWidth + 8,
                        tableTop - 20, "COURSE / SUBJECT");

                text(cs, PDType1Font.HELVETICA_BOLD, 10,
                        Color.WHITE,
                        margin + codeWidth + titleWidth + 5,
                        tableTop - 20, "UNITS");

                text(cs, PDType1Font.HELVETICA_BOLD, 10,
                        Color.WHITE, pageWidth - margin - gradeWidth + 8,
                        tableTop - 20, "GRADE");

                float currentY = tableTop - headerHeight;

                for (int i = 0; i < courses.size(); i++) {
                    CourseGrade course = courses.get(i);
                    currentY -= rowHeight;

                    cs.setNonStrokingColor(
                            i % 2 == 0 ? ROW_COLOR : BEIGE);
                    cs.addRect(margin, currentY,
                            usableWidth, rowHeight - 2);
                    cs.fill();

                    text(cs, PDType1Font.HELVETICA, 9,
                            BLACK, margin + 8, currentY + 9,
                            fit(course.code(), 11));

                    text(cs, PDType1Font.HELVETICA, 9,
                            BLACK, margin + codeWidth + 8,
                            currentY + 9,
                            fit(course.title(), 43));

                    text(cs, PDType1Font.HELVETICA, 9,
                            BLACK, margin + codeWidth + titleWidth + 12,
                            currentY + 9,
                            String.valueOf(course.units()));

                    text(cs, PDType1Font.HELVETICA_BOLD, 9,
                            GREEN, pageWidth - margin - gradeWidth + 10,
                            currentY + 9, fit(course.grade(), 8));

                    // Start another page when space runs low.
                    // For a full multi-page report, use a paginated layout.
                    if (currentY < 190 && i < courses.size() - 1) {
                        throw new IOException(
                                "Too many subjects for one page. "
                                + "Use a multi-page report layout.");
                    }
                }

                float scaleY = Math.min(currentY - 45, 150);

                text(cs, PDType1Font.HELVETICA_BOLD, 15,
                        GREEN, margin, scaleY, "GHC GRADE SCALE");

                cs.setStrokingColor(GREEN);
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, scaleY - 9);
                cs.lineTo(pageWidth - margin, scaleY - 9);
                cs.stroke();

                String[] scale = {
                    "1.00 = 98-100",
                    "1.25 = 91-97",
                    "1.50 = 85-90",
                    "1.75 = 79-84",
                    "2.00 = 73-78",
                    "2.25 = 67-72",
                    "2.50 = 61-66",
                    "2.75 = 55-60",
                    "3.00 = 50-54",
                    "5.00 = Below 50"
                };

                float scaleStartY = scaleY - 30;
                for (int i = 0; i < scale.length; i++) {
                    int column = i / 5;
                    int row = i % 5;

                    text(cs, PDType1Font.HELVETICA_BOLD, 9,
                            GREEN, margin + column * 175,
                            scaleStartY - row * 15, scale[i]);
                }

                text(cs, PDType1Font.HELVETICA_OBLIQUE, 8,
                        BLACK, margin, 38,
                        "Official student record | Great Hope Colleges");
            }

            document.save(output.toFile());
        }
    }

    private static void info(
            PDPageContentStream cs,
            float x,
            float y,
            String label,
            String value
    ) throws IOException {

        text(cs, PDType1Font.HELVETICA_BOLD, 9,
                GREEN, x, y, label);

        text(cs, PDType1Font.HELVETICA, 12,
                BLACK, x, y - 19,
                fit(value == null ? "" : value, 32));
    }

    private static void text(
            PDPageContentStream cs,
            PDType1Font font,
            float size,
            Color color,
            float x,
            float y,
            String value
    ) throws IOException {

        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(value));
        cs.endText();
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static String fit(String value, int maxLength) {
        String s = safe(value);
        if (s.length() <= maxLength) return s;
        return s.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
