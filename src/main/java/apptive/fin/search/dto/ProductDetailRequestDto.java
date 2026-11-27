package apptive.fin.search.dto;

import apptive.fin.search.enums.ProductCategoryEnum;

import java.util.List;

// 상품 상세 요청. 공개 정보 조회를 위해 모든 필드는 optional.
// productPropertyId: 클릭한 리스트 행의 property. 없으면 개인화 수익 지표를 잠근다.
// category: 목록 검색 요청에 보낸 대분류. 목록과 같은 조건으로 적합도·#최고이율을 계산하려고 받는다. 없으면 저축기간으로 정한다.
public record ProductDetailRequestDto(
        Long productPropertyId,
        List<OptionRequestDto> options,
        DetailedOptionsDto detailedOptions,
        ProductCategoryEnum category
) {
    public ProductDetailRequestDto(Long productPropertyId, List<OptionRequestDto> options, DetailedOptionsDto detailedOptions) {
        this(productPropertyId, options, detailedOptions, null);
    }
}
