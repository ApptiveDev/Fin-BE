package apptive.fin.search.service;

import apptive.fin.category.service.CategoryOptionService;
import apptive.fin.search.CategoryIdEnum;
import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.dto.*;
import apptive.fin.search.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final CategoryOptionService categoryOptionService;
    private final EligibilityFilterService eligibilityFilterService;
    private final MatchScoreService matchScoreService;
    private final RateCalculatorService rateCalculatorService;

    public ProductSearchResultDto search(SearchRequestDto request){
        // 거주지역 options에서 추출
        Map<Long, KeywordValueEnum> mapping = categoryOptionService.getOptionMap();
        KeywordValueEnum regionKeyword = request.options().stream()
                .filter(opt -> opt.categoryId().equals(CategoryIdEnum.REGION.getId()))
                .map(opt -> mapping.get(opt.optionId()))
                .filter(kw -> kw != null)
                .findFirst()
                .orElse(null);

        // 자격 필터링
        List<Product> eligible = eligibilityFilterService.filterEligible(request);

        // 거주 지역 필터링
        if (regionKeyword != null) {
            final KeywordValueEnum finalRegion = regionKeyword;
            eligible = eligible.stream()
                    .filter(p -> {
                        List<KeywordValueEnum> productRegions = p.getKeywords().stream()
                                .map(k -> k.getKeywordCode())
                                .filter(kw -> kw.name().startsWith("REGION_"))
                                .toList();
                        return productRegions.isEmpty()        // 전국 상품
                                || productRegions.contains(finalRegion); // 지역 일치
                    })
                    .toList();
        }

        // source별 분리
        List<Product> govList = eligible.stream()
                .filter(p -> p.getSource().getCode().equals("ONTONG")).toList();
        List<Product> bankList = eligible.stream()
                .filter(p -> p.getSource().getCode().equals("FSS")).toList();

        // 탭 A
        List<ProductMatchDto> govRanked = govList.stream()
                .map(p -> matchScoreService.score(p, request))
                .sorted(Comparator.comparingDouble(ProductMatchDto::totalScore).reversed())
                .toList();
        List<ProductMatchDto> bankRanked = bankList.stream()
                .map(p -> matchScoreService.score(p, request))
                .sorted(Comparator.comparingDouble(ProductMatchDto::totalScore).reversed())
                .toList();

        // 탭 B
        List<ProductRateDto> allRated = Stream.concat(govList.stream(), bankList.stream())
                .map(p -> rateCalculatorService.calculate(p, request)).toList();

        List<ProductRateDto> rateRanked = allRated.stream()
                .filter(r -> !r.isSubscription())
                .sorted(Comparator.comparingDouble(ProductRateDto::achievableRate).reversed())
                .toList();

        List<ProductRateDto> subscriptions = allRated.stream()
                .filter(ProductRateDto::isSubscription)
                .toList();

        // TODO : 계산 로직 추가 후 최종 완성할 부분
        return ProductSearchResultDto.builder()
                .governmentRanked(govRanked)
                .bankRanked(bankRanked)
                .rateRanked(rateRanked)
                .subscriptionProducts(subscriptions)
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

            Long categoryId = option.categoryId();
            // region 제거
            if (categoryId.equals(CategoryIdEnum.IDENTITY.getId())) identities.add(kw);
            else if(categoryId.equals(CategoryIdEnum.PERIOD.getId())) savingPeriod = kw;
            else if(categoryId.equals(CategoryIdEnum.BENEFIT.getId())) benefits.add(kw);
            else if(categoryId.equals(CategoryIdEnum.BANK_COND.getId())) bankConds.add(kw);
        }
        return new ResolvedKeywords(identities,savingPeriod, bankConds,benefits);
    }
    public record ResolvedKeywords(
            //region 제거
            List<KeywordValueEnum> identities,
            KeywordValueEnum savingPeriod,
            List<KeywordValueEnum> coreBenefits,
            List<KeywordValueEnum> bankConditions
    ){}
}
