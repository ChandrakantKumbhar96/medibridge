package com.medibridge.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Renders a Thymeleaf template to HTML, then converts that HTML to PDF.
 *
 * <p>Chosen over building PDFs imperatively (iText's low-level API) because the
 * layout stays editable as CSS - a header, a table, and page numbers are far
 * easier to adjust in a template than in drawing calls.
 *
 * <p>openhtmltopdf requires well-formed XHTML: every tag must be closed,
 * including {@code <br/>} and {@code <img/>}. Malformed markup throws at render
 * time rather than degrading, so the templates are written strictly.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    public byte[] render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate PDF from template " + templateName, e);
        }
    }
}
