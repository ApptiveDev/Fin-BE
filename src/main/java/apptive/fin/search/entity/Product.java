package apptive.fin.search.entity;

import apptive.fin.search.ProductType;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProductType type; // GOVERMENT, BANK

    private String providerName; // 은행명 or '정부'
    private String content;  // 상품 내용
    private String bankTerm;  // 은행 조건 텍스트

    private Double baseRate; // 기본 금리
    private Integer termMonths; // 저축 기간(개월)
    private Long maxMonthlyAmt; // 최대 월 납입액
    private Long minMonthlyAmt; // 최소 월 납입액
    // TODO: 최소 월 납입액은 제공이 안되는 경우가 있어서 default값 설정이 요구될듯 함.

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private final List<ProductKeyword> keywords = new ArrayList<>();
}
