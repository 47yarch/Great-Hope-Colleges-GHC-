
package com.ghc.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CourseDirectoryPdf {

    private static final Color GREEN = new Color(30, 77, 46);
    private static final Color BEIGE = new Color(250, 249, 244);
    private static final Color GOLD = new Color(185, 137, 39);
    private static final Color BORDER = new Color(210, 205, 190);
    private static final Color ALT_ROW = new Color(246, 244, 236);
    private static final Color BLACK = Color.BLACK;

    private static final float MARGIN = 35;
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = WIDTH - 2 * MARGIN;

    private CourseDirectoryPdf() {
    }

    public record CourseRow(
        String code,
        String name,
        String units,
        String description
    ) {
    }

    public static void generate(
            Path output,
            List<CourseRow> courses
    ) throws IOException {

        if (output == null || courses == null) {
            throw new IllegalArgumentException(
                "Output path and course list are required."
            );
        }

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        try (PDDocument document = new PDDocument()) {
            int rowsPerPage = 24;
            int totalPages = Math.max(
                1,
                (int) Math.ceil(
                    Math.max(1, courses.size()) / (double) rowsPerPage
                )
            );

            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs =
                         new PDPageContentStream(document, page)) {

                    background(cs);
                    header(cs);
                    summary(cs, courses);

                    float y = HEIGHT - 145;
                    y = tableHeader(cs, y);

                    int start = (pageNo - 1) * rowsPerPage;
                    int end = Math.min(
                        start + rowsPerPage,
                        courses.size()
                    );

                    for (int i = start; i < end; i++) {
                        row(cs, courses.get(i), i, y);
                        y -= 23;
                    }

                    if (pageNo == totalPages) {
                        approval(cs);
                    }

                    footer(cs, pageNo, totalPages);
                }
            }

            document.save(output.toFile());
        }
    }

    private static void background(
            PDPageContentStream cs
    ) throws IOException {
        cs.setNonStrokingColor(BEIGE);
        cs.addRect(0, 0, WIDTH, HEIGHT);
        cs.fill();
    }

    private static void header(
            PDPageContentStream cs
    ) throws IOException {
        cs.setNonStrokingColor(GREEN);
        cs.addRect(0, HEIGHT - 75, WIDTH, 75);
        cs.fill();

        text(cs, PDType1Font.HELVETICA_BOLD, 18,
            Color.WHITE, MARGIN, HEIGHT - 31,
            "GREAT HOPE COLLEGES (GHC)");

        text(cs, PDType1Font.HELVETICA, 9,
            Color.WHITE, MARGIN, HEIGHT - 49,
            "Courses / Subjects Export - Official Record");

        String date = LocalDate.now().format(
            DateTimeFormatter.ofPattern("MMMM d, yyyy")
        );

        text(cs, PDType1Font.HELVETICA, 9,
            Color.WHITE, WIDTH - 155, HEIGHT - 30,
            "Date: " + date);

        text(cs, PDType1Font.HELVETICA, 8,
            Color.WHITE, WIDTH - 155, HEIGHT - 46,
            "System: Academic Management");
    }

    private static void summary(
            PDPageContentStream cs,
            List<CourseRow> courses
    ) throws IOException {
        float y = HEIGHT - 120;
        float gap = 12;
        float cardWidth = (CONTENT_WIDTH - gap * 2) / 3;
        float cardHeight = 38;

        java.util.Set<String> codes = new java.util.HashSet<>();
        int totalUnits = 0;

        for (CourseRow course : courses) {
            codes.add(clean(course.code()));

            try {
                totalUnits += Integer.parseInt(
                    clean(course.units())
                );
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric unit values in the summary.
            }
        }

        card(cs, MARGIN, y, cardWidth, cardHeight,
            "TOTAL SUBJECTS", courses.size() + " Courses");

        card(cs, MARGIN + cardWidth + gap, y,
            cardWidth, cardHeight,
            "TOTAL UNITS", totalUnits + " Units");

        card(cs, MARGIN + (cardWidth + gap) * 2, y,
            cardWidth, cardHeight,
            "UNIQUE COURSE CODES", codes.size() + " Codes");
    }

    private static void card(
            PDPageContentStream cs,
            float x,
            float y,
            float w,
            float h,
            String label,
            String value
    ) throws IOException {
        cs.setNonStrokingColor(new Color(242, 239, 229));
        cs.addRect(x, y, w, h);
        cs.fill();

        cs.setStrokingColor(BORDER);
        cs.addRect(x, y, w, h);
        cs.stroke();

        cs.setNonStrokingColor(GREEN);
        cs.addRect(x, y, 5, h);
        cs.fill();

        text(cs, PDType1Font.HELVETICA, 7,
            GOLD, x + 12, y + 24, label);

        text(cs, PDType1Font.HELVETICA_BOLD, 10,
            GREEN, x + 12, y + 9, value);
    }

    private static float tableHeader(
            PDPageContentStream cs,
            float top
    ) throws IOException {
        float h = 25;
        float y = top - h;

        cs.setNonStrokingColor(GREEN);
        cs.addRect(MARGIN, y, CONTENT_WIDTH, h);
        cs.fill();

        float[] widths = {
            CONTENT_WIDTH * 0.19f,
            CONTENT_WIDTH * 0.34f,
            CONTENT_WIDTH * 0.12f,
            CONTENT_WIDTH * 0.35f
        };

        String[] labels = {
            "Course Code", "Course Name", "Units", "Description"
        };

        float x = MARGIN;

        for (int i = 0; i < labels.length; i++) {
            text(cs, PDType1Font.HELVETICA_BOLD, 9,
                Color.WHITE, x + 5, y + 9, labels[i]);
            x += widths[i];
        }

        return y;
    }

    private static void row(
            PDPageContentStream cs,
            CourseRow course,
            int index,
            float top
    ) throws IOException {
        float h = 23;
        float y = top - h;

        cs.setNonStrokingColor(
            index % 2 == 0 ? Color.WHITE : ALT_ROW
        );
        cs.addRect(MARGIN, y, CONTENT_WIDTH, h);
        cs.fill();

        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.45f);
        cs.addRect(MARGIN, y, CONTENT_WIDTH, h);
        cs.stroke();

        float[] widths = {
            CONTENT_WIDTH * 0.19f,
            CONTENT_WIDTH * 0.34f,
            CONTENT_WIDTH * 0.12f,
            CONTENT_WIDTH * 0.35f
        };

        String[] values = {
            course.code(), course.name(),
            course.units(), course.description()
        };

        float x = MARGIN;

        for (int i = 0; i < values.length; i++) {
            text(cs, PDType1Font.HELVETICA, 8,
                BLACK, x + 5, y + 8,
                fit(values[i], Math.max(8, (int) (widths[i] / 4.3f))));
            x += widths[i];
        }
    }

    private static void approval(
            PDPageContentStream cs
    ) throws IOException {
        float x = WIDTH - MARGIN - 175;
        float y = 65;

        cs.setStrokingColor(BLACK);
        cs.setLineWidth(1);
        cs.moveTo(x, y + 20);
        cs.lineTo(x + 175, y + 20);
        cs.stroke();

        centered(cs, x, y + 7, 175,
            "Approved / Verified By", BLACK);

        centered(cs, x, y - 7, 175,
            "Office of the Registrar", GOLD);
    }

    private static void centered(
            PDPageContentStream cs,
            float x,
            float y,
            float width,
            String value,
            Color color
    ) throws IOException {
        String safe = clean(value);
        float size = 8;
        float textWidth =
            PDType1Font.HELVETICA.getStringWidth(safe) / 1000 * size;

        text(cs, PDType1Font.HELVETICA, size, color,
            x + Math.max(0, (width - textWidth) / 2), y, safe);
    }

    private static void footer(
            PDPageContentStream cs,
            int page,
            int total
    ) throws IOException {
        cs.setStrokingColor(BORDER);
        cs.setLineWidth(0.6f);
        cs.moveTo(MARGIN, 35);
        cs.lineTo(WIDTH - MARGIN, 35);
        cs.stroke();

        text(cs, PDType1Font.HELVETICA, 7,
            Color.DARK_GRAY, MARGIN, 22,
            "Great Hope Colleges | Academic Records & Subject Directory");

        text(cs, PDType1Font.HELVETICA, 7,
            Color.DARK_GRAY, WIDTH - MARGIN - 60, 22,
            "Page " + page + " of " + total);
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

    private static String fit(String value, int maxLength) {
        String s = clean(value);

        if (s.length() <= maxLength) {
            return s;
        }

        return s.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
