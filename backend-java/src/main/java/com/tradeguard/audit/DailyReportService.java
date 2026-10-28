package com.tradeguard.audit;

import com.tradeguard.config.MarketProperties;
import com.tradeguard.order.Trade;
import com.tradeguard.order.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

@Service
public class DailyReportService {

    private final TradeRepository trades;
    private final EodReportRepository reports;
    private final MarketProperties market;

    public DailyReportService(TradeRepository trades, EodReportRepository reports,
                             MarketProperties market) {
        this.trades = trades;
        this.reports = reports;
        this.market = market;
    }

    @Transactional(readOnly = true)
    public byte[] generate(LocalDate date) {
        return reports.findById(date)
                .map(report -> report.getXmlContent().getBytes(StandardCharsets.UTF_8))
                .orElseGet(() -> createXml(date));
    }

    @Transactional
    public EodReport generateAndStore(LocalDate date) {
        byte[] xml = createXml(date);
        long tradeCount = trades.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                date.atStartOfDay(market.getZone()).toInstant(),
                date.plusDays(1).atStartOfDay(market.getZone()).toInstant()).size();
        var existing = reports.findById(date);
        EodReport report = existing.orElseGet(() ->
                new EodReport(date, tradeCount,
                        new String(xml, StandardCharsets.UTF_8), sha256(xml)));
        if (existing.isPresent()) {
            report.updateContent(tradeCount, new String(xml, StandardCharsets.UTF_8), sha256(xml));
        }
        return reports.saveAndFlush(report);
    }

    private byte[] createXml(LocalDate date) {
        ZoneId zone = market.getZone();
        var start = date.atStartOfDay(zone).toInstant();
        var end = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Trade> dailyTrades =
                trades.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                        start, end);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
            xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            xml.writeStartElement("dailyTradeReport");
            xml.writeAttribute("date", date.toString());
            xml.writeAttribute("timezone", zone.getId());
            xml.writeAttribute("tradeCount", Integer.toString(dailyTrades.size()));
            for (Trade trade : dailyTrades) {
                xml.writeStartElement("trade");
                element(xml, "id", trade.getId().toString());
                element(xml, "symbol", trade.getSymbol());
                element(xml, "buyOrderId", trade.getBuyOrderId().toString());
                element(xml, "sellOrderId", trade.getSellOrderId().toString());
                element(xml, "price", trade.getPrice().toPlainString());
                element(xml, "quantity", Long.toString(trade.getQuantity()));
                element(xml, "createdAt", trade.getCreatedAt().toString());
                xml.writeEndElement();
            }
            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
            return output.toByteArray();
        } catch (XMLStreamException ex) {
            throw new IllegalStateException("Could not generate the daily XML report.", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not close the daily XML report buffer.", ex);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", ex);
        }
    }

    private void element(XMLStreamWriter xml, String name, String value)
            throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }
}
