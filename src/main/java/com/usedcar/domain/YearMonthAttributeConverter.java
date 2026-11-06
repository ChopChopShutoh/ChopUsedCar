package com.usedcar.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/**
 * 初度登録年月（年月のみ）を "yyyy-MM" として保存するためのコンバータ。
 * 文字列比較でも時系列が保たれるため、ソート・範囲検索が正しく機能する。
 */
@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? null : YearMonth.parse(dbData);
    }
}
