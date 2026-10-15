
package com.ghc.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class StudentDirectoryPdf {

    private static final Color GREEN =
        new Color(30, 77, 46);

    private static final Color BEIGE =
        new Color(250, 249, 244);

    private static final Color GOLD =
        new Color(185, 137, 39);

    private static final Color BORDER =
        new Color(210, 205, 190);

    private static final Color ALT_ROW =
        new Color(246, 244, 236);

    private static final Color BLACK =
        Color.BLACK;

    private static final float MARGIN = 30;
    private static final float PAGE_WIDTH =
        PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT =
        PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH =
        PAGE_WIDTH - (MARGIN * 2);

    private StudentDirectoryPdf() {
    }

    public record StudentRow(
        String studentId,
        String firstName,
        String lastName,
        String gender,
        String birthDate,
        String email,
        String phone,
        String address,
        String yearLevel,
        String program,
        String status
    ) {
    }

    public static void generate(
            Path output,
            List<StudentRow> students
    ) throws IOException {

        if (output == null || students == null) {
            throw new IllegalArgumentException(
                "Output path and student list are required."
            );
        }

        if (output.getParent() != null) {
            java.nio.file.Files.createDirectories(output.getParent());
        }

        try (PDDocument document = new PDDocument()) {

            int pageNumber = 0;
            int totalPages = Math.max(
                1,
                (int) Math.ceil(
                    Math.max(1, students.size()) / 22.0
                )
            );

            for (int start = 0; start < students.size()
                    || (students.isEmpty() && start == 0);
                    start += 22) {

                pageNumber++;

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs =
                        new PDPageContentStream(document, page)) {

                    drawBackground(cs);
                    drawHeader(cs, pageNumber);
                    drawSummary(cs, students);

                    float y = PAGE_HEIGHT - 145;

                    y = drawTableHeader(cs, y);

                    int end = Math.min(
                        start + 22,
                        students.size()
                    );

                    for (int i = start; i < end; i++) {
                        drawStudentRow(
                            cs,
                            students.get(i),
                            i,
                            y
                        );
                        y -= 22;
                    }

                    if (pageNumber == totalPages) {
                        drawApproval(cs);
                    }

                    drawFooter(
                        cs,
                        pageNumber,
                        totalPages
                    );
                }

                if (students.isEmpty()) {
                    break;
                }
            }

            document.save(output.toFile());
        }
    }

    private static void drawBackground(
            PDPageContentStream cs
    ) throws IOException {

        cs.setNonStrokingColor(BEIGE);
        cs.addRect(
            0, 0, PAGE_WIDTH, PAGE_HEIGHT
        );
        cs.fill();
    }

    private static void drawHeader(
            PDPageContentStream cs,
            int pageNumber
    ) throws IOException {

        cs.setNonStrokingColor(GREEN);
        cs.addRect(
            0, PAGE_HEIGHT - 70,
            PAGE_WIDTH, 70
        );
        cs.fill();

        text(
            cs, PDType1Font.HELVETICA_BOLD,
            18, Color.WHITE,
            MARGIN, PAGE_HEIGHT - 30,
            "GREAT HOPE COLLEGES (GHC)"
        );

        text(
            cs, PDType1Font.HELVETICA,
            9, Color.WHITE,
            MARGIN, PAGE_HEIGHT - 48,
            "Student Directory Export - Official Record"
        );

        String date = LocalDate.now().format(
            DateTimeFormatter.ofPattern("MMMM d, yyyy")
        );

        text(
            cs, PDType1Font.HELVETICA,
            9, Color.WHITE,
            PAGE_WIDTH - 150,
            PAGE_HEIGHT - 29,
            "Date: " + date
        );

        text(
            cs, PDType1Font.HELVETICA,
            8, Color.WHITE,
            PAGE_WIDTH - 150,
            PAGE_HEIGHT - 45,
            "System: Academic Management"
        );
    }

    private static void drawSummary(
            PDPageContentStream cs,
            List<StudentRow> students
    ) throws IOException {

        float y = PAGE_HEIGHT - 112;
        float gap = 12;
        float cardWidth =
            (CONTENT_WIDTH - (gap * 2)) / 3;
        float cardHeight = 35;

        long active = students.stream()
            .filter(s -> "active".equalsIgnoreCase(s.status()))
            .count();

        String program = "N/A";
        if (!students.isEmpty()) {
            java.util.Map<String, Integer> counts =
                new java.util.LinkedHashMap<>();

            for (StudentRow s : students) {
                String p = clean(s.program());
                if (!p.isBlank()) {
                    counts.merge(p, 1, Integer::sum);
                }
            }

            program = counts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " ("
                    + Math.round(
                        e.getValue() * 100.0 / students.size()
                    ) + "%)")
                .orElse("N/A");
        }

        drawCard(
            cs, MARGIN, y, cardWidth, cardHeight,
            "TOTAL RECORDS", students.size() + " Students"
        );

        drawCard(
            cs, MARGIN + cardWidth + gap, y,
            cardWidth, cardHeight,
            "ACTIVE STATUS", active + " Active"
        );

        drawCard(
            cs, MARGIN + (cardWidth + gap) * 2, y,
            cardWidth, cardHeight,
            "PRIMARY PROGRAM", fit(program, 24)
        );
    }

    private static void drawCard(
            PDPageContentStream cs,
            float x,
            float y,
            float width,
            float height,
            String label,
            String value
    ) throws IOException {

        cs.setNonStrokingColor(
            new Color(242, 239, 229)
        );
        cs.addRect(x, y, width, height);
        cs.fill();

        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.6f);
        cs.addRect(x, y, width, height);
        cs.stroke();

        cs.setNonStrokingColor(GREEN);
        cs.addRect(x, y, 5, height);
        cs.fill();

        text(
            cs, PDType1Font.HELVETICA,
            7, GOLD, x + 12, y + 22,
            label
        );

        text(
            cs, PDType1Font.HELVETICA_BOLD,
            10, GREEN, x + 12, y + 8,
            fit(value, 28)
        );
    }

    private static float drawTableHeader(
            PDPageContentStream cs,
            float top
    ) throws IOException {

        float height = 24;
        float y = top - height;

        cs.setNonStrokingColor(GREEN);
        cs.addRect(
            MARGIN, y, CONTENT_WIDTH, height
        );
        cs.fill();

        String[] headers = {
            "Student ID", "First Name", "Last Name",
            "Gender", "Birth Date", "Email",
            "Phone", "Address", "Yr Level",
            "Program", "Status"
        };

        float[] widths = {
            59, 50, 49, 35, 48, 65,
            46, 61, 39, 44, 43
        };

        float x = MARGIN;

        for (int i = 0; i < headers.length; i++) {
            text(
                cs, PDType1Font.HELVETICA_BOLD,
                6.5f, Color.WHITE,
                x + 3, y + 9,
                headers[i]
            );
            x += widths[i];
        }

        return y;
    }

    private static void drawStudentRow(
            PDPageContentStream cs,
            StudentRow s,
            int index,
            float top
    ) throws IOException {

        float height = 22;
        float y = top - height;

        cs.setNonStrokingColor(
            index % 2 == 0 ? Color.WHITE : ALT_ROW
        );
        cs.addRect(
            MARGIN, y, CONTENT_WIDTH, height
        );
        cs.fill();

        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.35f);
        cs.addRect(
            MARGIN, y, CONTENT_WIDTH, height
        );
        cs.stroke();

        String[] values = {
            s.studentId(), s.firstName(), s.lastName(),
            s.gender(), s.birthDate(), s.email(),
            s.phone(), s.address(), s.yearLevel(),
            s.program(), s.status()
        };

        float[] widths = {
            59, 50, 49, 35, 48, 65,
            46, 61, 39, 44, 43
        };

        float x = MARGIN;

        for (int i = 0; i < values.length; i++) {
            Color color = BLACK;

            if (i == 10
                    && "active".equalsIgnoreCase(values[i])) {
                color = new Color(20, 115, 55);
            }

            text(
                cs, PDType1Font.HELVETICA,
                6.3f, color,
                x + 3, y + 8,
                fit(values[i], maxChars(widths[i]))
            );

            x += widths[i];
        }
    }

    private static int maxChars(float width) {
        return Math.max(5, (int) (width / 3.7f));
    }

    private static void drawApproval(
            PDPageContentStream cs
    ) throws IOException {

        float x = PAGE_WIDTH - MARGIN - 175;
        float y = 65;

        cs.setStrokingColor(BLACK);
        cs.setLineWidth(1);
        cs.moveTo(x, y + 20);
        cs.lineTo(x + 175, y + 20);
        cs.stroke();

        centeredText(
            cs, PDType1Font.HELVETICA,
            8, BLACK, x, y + 7, 175,
            "Approved / Verified By"
        );

        centeredText(
            cs, PDType1Font.HELVETICA,
            8, GOLD, x, y - 7, 175,
            "Office of the Registrar"
        );
    }

    private static void drawFooter(
            PDPageContentStream cs,
            int page,
            int total
    ) throws IOException {

        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.6f);
        cs.moveTo(MARGIN, 35);
        cs.lineTo(PAGE_WIDTH - MARGIN, 35);
        cs.stroke();

        text(
            cs, PDType1Font.HELVETICA,
            7, Color.DARK_GRAY,
            MARGIN, 22,
            "Great Hope Colleges | Academic Records & Student Directory System"
        );

        String pageText = "Page " + page + " of " + total;

        text(
            cs, PDType1Font.HELVETICA,
            7, Color.DARK_GRAY,
            PAGE_WIDTH - MARGIN - 65, 22,
            pageText
        );
    }

    private static void centeredText(
            PDPageContentStream cs,
            PDType1Font font,
            float size,
            Color color,
            float x,
            float y,
            float width,
            String value
    ) throws IOException {

        String safe = clean(value);
        float textWidth =
            font.getStringWidth(safe) / 1000 * size;

        text(
            cs, font, size, color,
            x + Math.max(0, (width - textWidth) / 2),
            y, safe
        );
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
        cs.showText(clean(value));
        cs.endText();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static String fit(
            String value,
            int maxLength
    ) {
        String s = clean(value);

        if (s.length() <= maxLength) {
            return s;
        }

        return s.substring(
            0, Math.max(0, maxLength - 3)
        ) + "...";
    }
}
