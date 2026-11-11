package apptive.fin.category.service;

import apptive.fin.category.dto.*;
import apptive.fin.category.repository.CategoryRepository;
import apptive.fin.global.error.BusinessException;
import apptive.fin.search.enums.CategoryIdEnum;
import apptive.fin.search.enums.KeywordValueEnum;
import apptive.fin.search.SearchErrorCode;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ResolvedKeywords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    // 숨김 옵션은 저장된 선택(selected_option_ids)과 레거시 매칭 로직이 남아 있어 DB에서 지우지 않고 응답에서만 뺀다.
    // 첫거래 고객, 재예치는 거래이력 입력(neverUsedBanks, maturedSavingBanks)으로 자동 활성화된다.
    private static final Set<String> HIDDEN_OPTION_CODES = Set.of(
            "TERM_OVER_3_YEARS",
            "TERM_2_TO_3_YEARS",
            "TERM_AROUND_1_YEAR",
            "BANK_FIRST_TRANSACTION",
            "BANK_REDEPOSIT"
    );

    // 상품관심사는 다시 쓸 수 있어 행을 남겨 둔다.
    private static final Set<Long> HIDDEN_CATEGORY_IDS = Set.of(
            CategoryIdEnum.INTEREST.getId()
    );

    private final CategoryRepository categoryRepository;

    public List<CategoryResponseDto> getCategories() {
        List<CategoryFlatDto> flats = categoryRepository.findAllWithOptions();

        Map<Long, List<CategoryFlatDto>> grouped =
                flats.stream()
                        .filter(f -> !HIDDEN_CATEGORY_IDS.contains(f.categoryId()))
                        .filter(f -> !HIDDEN_OPTION_CODES.contains(f.optionCode()))
                        .collect(Collectors.groupingBy(
                                CategoryFlatDto::categoryId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
        return grouped.values().stream()
                .map(list -> {
                    if (list.isEmpty()) return null;

                    Long categoryId = list.get(0).categoryId();
                    String categoryName = list.get(0).categoryName();

                    List<OptionDto> options = list.stream()
                            .map(f -> new OptionDto(
                                    f.optionId(),
                                    f.optionValue()
                            ))
                            .toList();
                    return new CategoryResponseDto(categoryId, categoryName, options);
                })
                .toList();
    }



}
