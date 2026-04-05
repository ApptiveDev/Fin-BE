package apptive.fin.search;

import apptive.fin.category.service.CategoryOptionService;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.DynamicFormResponseDto;
import apptive.fin.search.dto.MedianIncomesDto;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.service.DynamicFormService;
import apptive.fin.search.service.MedianIncomeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicFormServiceTest {

    @Mock
    private MedianIncomeService medianIncomeService;

    @Mock
    private CategoryOptionService categoryOptionService;

    @InjectMocks
    private DynamicFormService dynamicFormService;

    @Test
    void 미취업_군복무_은행정보_가구원수를_포함하면_동적폼조건을_함께_반영한다() {
        int currentYear = LocalDateTime.now().getYear();
        MedianIncomesDto medianIncomesDto = MedianIncomesDto.builder()
                .year(currentYear)
                .householdSize(3)
                .p60(200)
                .p80(300)
                .p100(400)
                .p120(500)
                .p150(600)
                .p180(700)
                .build();
        SearchRequestDto request = new SearchRequestDto(
                List.of(
                        new OptionRequestDto(1L, 10L),
                        new OptionRequestDto(2L, 20L)
                ),
                createDetailedOptions(3, List.of("KB국민은행"))
        );

        when(categoryOptionService.getOptionMap()).thenReturn(Map.of(
                10L, KeywordValueEnum.STATUS_UNEMPLOYED,
                20L, KeywordValueEnum.STATUS_MILITARY
        ));
        when(medianIncomeService.getMedianIncomesDto(currentYear, 3)).thenReturn(medianIncomesDto);

        DynamicFormResponseDto result = dynamicFormService.calcFormCondition(request);

        assertThat(result.yearlyEarnDefault()).isEqualTo(0);
        assertThat(result.ageBound()).isEqualTo(39);
        assertThat(result.showBankInterestRateCheckList()).isTrue();
        assertThat(result.medianIncomes()).isEqualTo(medianIncomesDto);
        assertThat(result.showTenure()).isTrue();
        assertThat(result.preferentialInterestRateOptions()).isEmpty();

        verify(medianIncomeService).getMedianIncomesDto(currentYear, 3);
    }

    @Test
    void 추가조건이_없으면_기본값으로_응답한다() {
        SearchRequestDto request = new SearchRequestDto(
                List.of(),
                createDetailedOptions(null, List.of())
        );

        when(categoryOptionService.getOptionMap()).thenReturn(Map.of());

        DynamicFormResponseDto result = dynamicFormService.calcFormCondition(request);

        assertThat(result.showTenure()).isTrue();
        assertThat(result.ageBound()).isEqualTo(34);
        assertThat(result.yearlyEarnDefault()).isNull();
        assertThat(result.showBankInterestRateCheckList()).isFalse();
        assertThat(result.medianIncomes()).isNull();
        assertThat(result.preferentialInterestRateOptions()).isEmpty();

        verifyNoInteractions(medianIncomeService);
    }

    @Test
    void 키워드로_매핑되지_않는_옵션은_무시한다() {
        SearchRequestDto request = new SearchRequestDto(
                List.of(new OptionRequestDto(1L, 999L)),
                createDetailedOptions(null, List.of())
        );

        when(categoryOptionService.getOptionMap()).thenReturn(Map.of());

        DynamicFormResponseDto result = dynamicFormService.calcFormCondition(request);

        assertThat(result.showTenure()).isTrue();
        assertThat(result.ageBound()).isEqualTo(34);
        assertThat(result.yearlyEarnDefault()).isNull();
        assertThat(result.showBankInterestRateCheckList()).isFalse();

        verifyNoInteractions(medianIncomeService);
    }

    private DetailedOptionsDto createDetailedOptions(Integer householdSize, List<String> mainBanks) {
        return new DetailedOptionsDto(
                LocalDate.of(2000, 1, 1),
                30_000_000L,
                householdSize,
                null,
                null,
                null,
                null,
                null,
                null,
                mainBanks,
                List.of()
        );
    }
}
