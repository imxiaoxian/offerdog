package com.hanserdev.interview.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class YearMonthFlexibleDeserializer extends JsonDeserializer<YearMonth> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public YearMonth deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return parseFromString(p.getText(), ctxt);
        }
        if (token == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            JsonNode yearNode = node.get("year");
            JsonNode monthNode = node.get("month");
            if (yearNode != null && monthNode != null && yearNode.isInt() && monthNode.isInt()) {
                return YearMonth.of(yearNode.asInt(), monthNode.asInt());
            }
            throw ctxt.weirdStringException(node.toString(), YearMonth.class,
                    "JSON 对象必须包含整型的 year 和 month 字段");
        }
        return (YearMonth) ctxt.handleUnexpectedToken(YearMonth.class, p);
    }

    private YearMonth parseFromString(String value, DeserializationContext ctxt) throws IOException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw ctxt.weirdStringException(value, YearMonth.class, "YearMonth 格式必须为 yyyy-MM");
        }
    }
}
