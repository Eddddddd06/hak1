package com.example.hack1.service;

import com.oreo.insight.domain.Sale;
import com.oreo.insight.dto.WeeklySummaryRequest;
import com.oreo.insight.events.PremiumReportRequestedEvent;
import com.oreo.insight.events.ReportRequestedEvent;
import com.oreo.insight.repository.SaleRepository;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import com.oreo.insight.dto.WeeklyPremiumRequest;

@Service
public class WeeklySummaryService {

    private final SaleRepository saleRepository;
    private final SalesAggregationService aggregationService;
    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public WeeklySummaryService(SaleRepository saleRepository,
                                SalesAggregationService aggregationService,
                                JavaMailSender mailSender) {
        this.saleRepository = saleRepository;
        this.aggregationService = aggregationService;
        this.mailSender = mailSender;
    }

    // Manejo asíncrono vía eventos, como exige el README
    @Async
    @EventListener
    public void handlePremiumReport(PremiumReportRequestedEvent event) {
        Instant from = event.getFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = event.getTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var page = saleRepository.findByDateRange(from, toExclusive, org.springframework.data.domain.Pageable.unpaged());
        List<com.oreo.insight.domain.Sale> sales = page.getContent();
        if (event.getBranch() != null && !event.getBranch().isBlank()) {
            sales = sales.stream().filter(s -> event.getBranch().equals(s.getBranch())).toList();
        }
        SalesAggregationService.AggregationResult agg = aggregationService.aggregate(sales);
    
        String llmSummary = maybeCallGithubModels(buildPrompt(agg, from, toExclusive, event.getBranch()));
        if (llmSummary == null || llmSummary.isBlank()) {
            llmSummary = buildFallbackSummary(agg, from, toExclusive, event.getBranch());
        }
    
        String html = buildHtmlReport(llmSummary, agg, event.isIncludeCharts());
    
        byte[] pdf = null;
        if (event.isAttachPdf()) {
            try {
                pdf = generateSimplePdf("Reporte Semanal Oreo", agg);
            } catch (IOException ignored) {}
        }
        sendEmailHtml(event.getEmailTo(), "Reporte Semanal Oreo (Premium) - " + event.getFrom() + " a " + event.getTo(), html, pdf, pdf != null ? "reporte-semanal.pdf" : null);
    }
    
    private String buildHtmlReport(String summary, SalesAggregationService.AggregationResult agg, boolean includeCharts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>Resumen Semanal Oreo</h2>");
        sb.append("<p>" + escapeHtml(summary) + "</p>");
        sb.append("<h3>Métricas</h3>");
        sb.append("<ul>");
        sb.append("<li>Total unidades: " + agg.totalUnits() + "</li>");
        sb.append("<li>Ingresos totales: $" + agg.totalRevenue() + "</li>");
        sb.append("<li>Top SKU: " + safe(agg.topSkuByUnits()) + "</li>");
        sb.append("<li>Top Sucursal: " + safe(agg.topBranchByUnits()) + "</li>");
        sb.append("</ul>");
        if (includeCharts) {
            String chartUrl = quickChartBar("Unidades por SKU", agg.unitsBySku());
            sb.append("<h3>Gráfico: Unidades por SKU</h3>");
            sb.append("<img alt='chart' src='" + chartUrl + "' style='max-width:600px'>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
    
    private String quickChartBar(String title, java.util.Map<String, Integer> data) {
        List<String> labels = new java.util.ArrayList<>(data.keySet());
        List<Integer> values = labels.stream().map(data::get).toList();
        String json = "{\"type\":\"bar\",\"data\":{\"labels\":" + toJsonArray(labels) + ",\"datasets\":[{\"label\":\"" + escapeJson(title) + "\",\"data\":" + values.toString() + "}]}}";
        try {
            String enc = java.net.URLEncoder.encode(json, java.nio.charset.StandardCharsets.UTF_8);
            return "https://quickchart.io/chart?c=" + enc;
        } catch (Exception e) {
            return "https://quickchart.io/chart?c=" + json; // fallback sin encode
        }
    }
    
    private String toJsonArray(List<String> labels) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escapeJson(labels.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
    
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    private byte[] generateSimplePdf(String title, SalesAggregationService.AggregationResult agg) throws IOException {
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);
        org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
        var font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
        cs.beginText();
        cs.setFont(font, 16);
        cs.newLineAtOffset(50, 750);
        cs.showText(title);
        cs.endText();
    
        cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12); cs.newLineAtOffset(50, 720);
        cs.showText("Total unidades: " + agg.totalUnits()); cs.endText();
        cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12); cs.newLineAtOffset(50, 700);
        cs.showText("Ingresos totales: $" + agg.totalRevenue()); cs.endText();
        cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12); cs.newLineAtOffset(50, 680);
        cs.showText("Top SKU: " + safe(agg.topSkuByUnits())); cs.endText();
        cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12); cs.newLineAtOffset(50, 660);
        cs.showText("Top Sucursal: " + safe(agg.topBranchByUnits())); cs.endText();
    
        cs.close();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }
    
    private String safe(String s) { return s == null ? "-" : s; }
    
    private void sendEmailHtml(String to, String subject, String html, byte[] pdfAttachment, String attachName) {
        if (to == null || to.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, pdfAttachment != null);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (pdfAttachment != null && attachName != null) {
                helper.addAttachment(attachName, new org.springframework.core.io.ByteArrayResource(pdfAttachment));
            }
            mailSender.send(message);
        } catch (Exception ignored) {}
    }

    private String buildPrompt(SalesAggregationService.AggregationResult agg, Instant from, Instant to, String branch) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un analista de ventas de Oreo. Genera un resumen claro y accionable de la semana.\\n");
        sb.append("Rango: ").append(from).append(" a ").append(to).append("\\n");
        if (branch != null && !branch.isBlank()) sb.append("Sucursal: ").append(branch).append("\\n");
        sb.append("Total unidades: ").append(agg.totalUnits()).append("\\n");
        sb.append("Ingresos totales: $").append(agg.totalRevenue()).append("\\n");
        sb.append("Top SKU por unidades: ").append(agg.topSkuByUnits()).append("\\n");
        sb.append("Top Sucursal por unidades: ").append(agg.topBranchByUnits()).append("\\n");
        sb.append("Incluye 3 recomendaciones concretas para elevar ventas.\\n");
        return sb.toString();
    }

    private String buildFallbackSummary(SalesAggregationService.AggregationResult agg, Instant from, Instant to, String branch) {
        String br = (branch != null && !branch.isBlank()) ? (" en sucursal " + branch) : "";
        return "Resumen de ventas del "+from+" al "+to+br+": " +
                "Unidades: " + agg.totalUnits() + ", Ingresos: $" + agg.totalRevenue() + ". " +
                "Top SKU: " + agg.topSkuByUnits() + ", Top Sucursal: " + agg.topBranchByUnits() + ".";
    }

    private String maybeCallGithubModels(String prompt) {
        String ghToken = System.getenv("GITHUB_TOKEN");
        String ghUrl = System.getenv("GITHUB_MODELS_URL");
        String modelId = System.getenv("MODEL_ID");
        String apiKey = System.getenv("GITHUB_MODELS_API_KEY");
        String endpoint = System.getenv().getOrDefault("GITHUB_MODELS_ENDPOINT_URL", "https://models.inference.ai.azure.com");

        // Seleccionar credenciales y endpoint según lo disponible
        String authHeaderName;
        String authHeaderValue;
        String finalUrl;
        if (ghToken != null && !ghToken.isBlank()) {
            authHeaderName = "Authorization";
            authHeaderValue = "Bearer " + ghToken;
            finalUrl = (ghUrl != null && !ghUrl.isBlank()) ? ghUrl : "https://models.github.ai/inference/chat/completions";
        } else if (apiKey != null && !apiKey.isBlank()) {
            authHeaderName = "api-key";
            authHeaderValue = apiKey;
            finalUrl = endpoint + "/v1/chat/completions";
        } else {
            return null; // no credenciales
        }

        String model = (modelId != null && !modelId.isBlank()) ? modelId : "gpt-4o-mini";
        try {
            String body = "{\n" +
                    "  \"model\": \"" + model + "\",\n" +
                    "  \"messages\": [{\n" +
                    "    \"role\": \"user\",\n" +
                    "    \"content\": [{\n" +
                    "      \"type\": \"text\",\n" +
                    "      \"text\": " + jsonEscape(prompt) + "\n" +
                    "    }]\n" +
                    "  }]\n" +
                    "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("Content-Type", "application/json")
                    .header(authHeaderName, authHeaderValue)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractTextFromOpenAIResponse(response.body());
            }
        } catch (IOException | InterruptedException ignored) {}
        return null;
    }

    private String jsonEscape(String s) {
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return '"' + escaped + '"';
    }

    private String extractTextFromOpenAIResponse(String body) {
        // Implementación muy sencilla: buscar "\"content\":\"...\"" y devolver el contenido
        int idx = body.indexOf("\"content\":");
        if (idx < 0) return null;
        int start = body.indexOf('"', idx + 10);
        if (start < 0) return null;
        int end = body.indexOf('"', start + 1);
        if (end < 0) return null;
        return body.substring(start + 1, end);
    }

    private void sendEmail(String to, String subject, String text) {
        if (to == null || to.isBlank()) return; // si no hay destinatario, no enviamos
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        try {
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }

    @Async
    @EventListener
    public void handleReportRequest(ReportRequestedEvent event) {
        Instant from = event.getFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = event.getTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var page = saleRepository.findByDateRange(from, toExclusive, org.springframework.data.domain.Pageable.unpaged());
        List<com.oreo.insight.domain.Sale> sales = page.getContent();
        if (event.getBranch() != null && !event.getBranch().isBlank()) {
            sales = sales.stream().filter(s -> event.getBranch().equals(s.getBranch())).toList();
        }
        SalesAggregationService.AggregationResult agg = aggregationService.aggregate(sales);
        String prompt = buildPrompt(agg, from, toExclusive, event.getBranch());
        String summary = maybeCallGithubModels(prompt);
        if (summary == null || summary.isBlank()) {
            summary = buildFallbackSummary(agg, from, toExclusive, event.getBranch());
        }
        sendEmail(event.getEmailTo(), "Reporte Semanal Oreo - " + event.getFrom() + " a " + event.getTo(), summary);
    }

    public String enqueuePremiumSummary(WeeklyPremiumRequest req) {
        LocalDate to = req.getTo() != null ? req.getTo() : LocalDate.now();
        LocalDate from = req.getFrom() != null ? req.getFrom() : to.minusDays(7);
        String requestId = UUID.randomUUID().toString();
        applicationEventPublisher.publishEvent(new com.oreo.insight.events.PremiumReportRequestedEvent(
                requestId,
                from,
                to,
                req.getBranch(),
                req.getEmailTo(),
                req.isIncludeCharts(),
                req.isAttachPdf()
        ));
        return requestId;
    }

    public String enqueueSummary(WeeklySummaryRequest req) {
        LocalDate to = req.getTo() != null ? req.getTo() : LocalDate.now();
        LocalDate from = req.getFrom() != null ? req.getFrom() : to.minusDays(7);
        String requestId = UUID.randomUUID().toString();
        applicationEventPublisher.publishEvent(new com.oreo.insight.events.ReportRequestedEvent(
                requestId,
                from,
                to,
                req.getBranch(),
                req.getEmailTo()
        ));
        return requestId;
    }
}