package pdfgen;

import java.io.*;
import java.time.LocalDateTime;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

@WebServlet("/pdfgenserv")
public class PdfGenServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static class CustomFooterEvent extends PdfPageEventHelper {
        Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
        Font pageNumberFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(),
                Element.ALIGN_LEFT,
                new Phrase("Faculty Document", watermarkFont),
                document.left() + 10,
                document.bottom() - 10,
                0);

            ColumnText.showTextAligned(writer.getDirectContent(),
                Element.ALIGN_RIGHT,
                new Phrase(String.format("Page %d", writer.getPageNumber()), pageNumberFont),
                document.right() - 10,
                document.bottom() - 10,
                0);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] pageTitles = request.getParameterValues("pageTitles");
        String[] pageContents = request.getParameterValues("pageContents");

        String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        String fileName = "FacultyDocument_" + timestamp + ".pdf";
        String filePath = getServletContext().getRealPath("/") + fileName;

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            writer.setPageEvent(new CustomFooterEvent());
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 26, Font.BOLD, new BaseColor(0, 70, 140));
            Font headingFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Font subheadingFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLDITALIC);
            Font bulletFont = new Font(Font.FontFamily.HELVETICA, 14);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 13);

            if (pageTitles == null || pageContents == null || pageTitles.length == 0 || pageContents.length == 0) {
                throw new IllegalArgumentException("No page data provided.");
            }

            if (pageTitles.length != pageContents.length) {
                throw new IllegalArgumentException("Mismatch in page titles and contents.");
            }

            for (int i = 0; i < pageTitles.length; i++) {
                if (i > 0) {
                    document.newPage();
                }

                String currentTitle = pageTitles[i];
                String currentContent = pageContents[i];

                if (currentTitle != null && !currentTitle.isEmpty()) {
                    Paragraph docTitle = new Paragraph(currentTitle, titleFont);
                    docTitle.setAlignment(Element.ALIGN_CENTER);
                    docTitle.setSpacingAfter(20);
                    document.add(docTitle);
                }

                com.itextpdf.text.List bulletList = null;
                String[] lines = (currentContent != null) ? currentContent.split("\n") : new String[0];

                for (String line : lines) {
                    line = line.trim();

                    if (line.startsWith("###")) {
                        if (bulletList != null) {
                            document.add(bulletList);
                            bulletList = null;
                        }
                        document.add(new Paragraph(line.replaceFirst("###", "").trim(), headingFont));
                    } else if (line.startsWith("##")) {
                        if (bulletList != null) {
                            document.add(bulletList);
                            bulletList = null;
                        }
                        document.add(new Paragraph(line.replaceFirst("##", "").trim(), subheadingFont));
                    } else if (line.startsWith("..")) {
                        if (bulletList == null) {
                            bulletList = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
                            bulletList.setIndentationLeft(30);
                        }
                        bulletList.add(new ListItem(line.replaceFirst("\\.\\.", "").trim(), bulletFont));
                    } else {
                        if (bulletList != null) {
                            document.add(bulletList);
                            bulletList = null;
                        }
                        document.add(new Paragraph(line, normalFont));
                    }
                }

                if (bulletList != null) {
                    document.add(bulletList);
                }
            }

            document.close();
            response.sendRedirect("download.html?file=" + fileName);

        } catch (Exception e) {
            response.setContentType("text/html");
            response.getWriter().println("Error generating PDF: " + e.getMessage());
        }
    }
}
