package com.apparel.commerce.catalog;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "size_chart")
@Getter
@Setter
public class SizeChart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** e.g. [{"size":"M","chestCm":[95,101],"heightCm":[168,176]}] */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> rows;

    private String fitNotes;
}
