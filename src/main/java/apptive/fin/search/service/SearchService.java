package apptive.fin.search.service;

import apptive.fin.category.repository.CategoryOptionRepository;
import apptive.fin.category.service.CategoryOptionService;
import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ProductSearchResultDto;
import apptive.fin.search.dto.SearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final CategoryOptionRepository categoryOptionRepository;
    private final CategoryOptionService categoryOptionService;

    public ProductSearchResultDto search(SearchRequestDto request){
        ResolvedKeywords keywords = resolveKeywords (request.options());

        // TODO : 계산 로직 추가 후 최종 완성할 부분
        return ProductSearchResultDto.builder()
                .governmentRanked(List.of())
                .bankRanked(List.of())
                .rateRanked(List.of())
                .subscriptionProducts(List.of())
                .build();
    }

    private ResolvedKeywords resolveKeywords(List<OptionRequestDto> options){
        Map<Long, KeywordValueEnum> mapping = categoryOptionService.getOptionMap();

        List<KeywordValueEnum> regions = new ArrayList<>();
        List<KeywordValueEnum> identities = new ArrayList<>();
        KeywordValueEnum savingPeriod = null;
        List<KeywordValueEnum> benefits = new ArrayList<>();
        List<KeywordValueEnum> bankConds = new ArrayList<>();

        for (OptionRequestDto option : options){
            KeywordValueEnum kw = mapping.get(option.optionId());
            if(kw == null) continue;

            switch(option.categoryId().intValue()){
                case 1 -> regions.add(kw);
                case 2 -> identities.add(kw);
                case 3 -> savingPeriod = kw;
                case 4 -> bankConds.add(kw);
                case 6 -> benefits.add(kw);

            }
        }
        return new ResolvedKeywords(regions,identities,savingPeriod, bankConds,benefits);
    }
    public record ResolvedKeywords(
            List<KeywordValueEnum> regions,
            List<KeywordValueEnum> identities,
            KeywordValueEnum savingPeriod,
            List<KeywordValueEnum> coreBenefits,
            List<KeywordValueEnum> bankConditions
    ){}
}
