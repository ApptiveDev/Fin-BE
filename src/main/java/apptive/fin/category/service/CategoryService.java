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

    private static final Set<String> HIDDEN_OPTION_CODES = Set.of(
            "TERM_OVER_3_YEARS",
            "TERM_2_TO_3_YEARS",
            "TERM_AROUND_1_YEAR",
            "BANK_FIRST_TRANSACTION",
            "BANK_REDEPOSIT"
    );

    private static final Map<String, String> DISPLAY_VALUE_MAP = Map.of(
            "STATUS_UNEMPLOYED", "미취업",
            "STATUS_PART_TIME", "알바/프리랜서",
            "BENEFIT_MAX_INTEREST", "최고이율 중심",
            "BANK_AUTO_TRANSFER", "자동이체 우대",
            "BANK_CARD_USAGE", "카드실적 우대",
            "BANK_SALARY_TRANSFER", "급여이체 우대",
            "BANK_MARKETING", "마케팅동의 OK"
    );

    private static final Map<String, String> CATEGORY_NAME_MAP = Map.of(
            "우대거래", "은행 우대 조건",
            "혜택선택", "핵심 혜택"
    );

    private final CategoryRepository categoryRepository;

    public List<CategoryResponseDto> getCategories() {
        List<CategoryFlatDto> flats = categoryRepository.findAllWithOptions();

        Map<Long, List<CategoryFlatDto>> grouped =
                flats.stream()
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
                    String categoryName = CATEGORY_NAME_MAP.getOrDefault(
                            list.get(0).categoryName(), list.get(0).categoryName());

                    List<OptionDto> options = list.stream()
                            .map(f -> new OptionDto(
                                    f.optionId(),
                                    DISPLAY_VALUE_MAP.getOrDefault(f.optionCode(), f.optionValue())
                            ))
                            .toList();
                    return new CategoryResponseDto(categoryId, categoryName, options);
                })
                .toList();
    }



}
