package learning.fintech.report;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class AuditPdfReport {
    public byte[] render(PaymentEvent event, ReportDecision decision) {
        String content = "BT /F1 12 Tf 54 740 Td "
                + pdfText("Payment learning report") + " Tj 0 -28 Td "
                + pdfText("Payment: " + event.paymentId()) + " Tj 0 -18 Td "
                + pdfText("Course: " + event.courseName()) + " Tj 0 -18 Td "
                + pdfText("Amount: " + event.amountMinor() + " " + event.currency()) + " Tj 0 -18 Td "
                + pdfText("Status: " + event.status()) + " Tj 0 -18 Td "
                + pdfText("Occurred: " + DateTimeFormatter.ISO_INSTANT.format(event.occurredAt().atOffset(ZoneOffset.UTC)))
                + " Tj 0 -18 Td " + pdfText("Audit reason: " + decision.auditReason()) + " Tj ET";

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + content + "\nendstream");

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        write(pdf, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            write(pdf, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xref = pdf.size();
        write(pdf, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }
        write(pdf, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    private static String pdfText(String value) {
        String ascii = value.replaceAll("[^\\x20-\\x7E]", "?");
        return "(" + ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") + ")";
    }

    private static void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }
}

