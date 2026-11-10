package apptive.fin.category;

import apptive.fin.category.dto.*;
import apptive.fin.category.repository.CategoryRepository;
import apptive.fin.category.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void 카테고리_옵션_조회_성공() {
        List<CategoryFlatDto> mockData = List.of(
                new CategoryFlatDto(1L, "거주지역", 1L, "서울", "REGION_SEOUL"),
                new CategoryFlatDto(1L, "거주지역", 2L, "부산", "REGION_BUSAN"),
                new CategoryFlatDto(2L, "현재신분", 3L, "미취업자", "STATUS_UNEMPLOYED")
        );

        given(categoryRepository.findAllWithOptions()).willReturn(mockData);

        List<CategoryResponseDto> result = categoryService.getCategories();

        assertThat(result).hasSize(2);

        // 첫 번째 카테고리 검증
        CategoryResponseDto category1 = result.get(0);
        assertThat(category1.categoryId()).isEqualTo(1L);
        assertThat(category1.categoryName()).isEqualTo("거주지역");
        assertThat(category1.options()).hasSize(2);

        assertThat(category1.options().get(0).optionValue()).isEqualTo("서울");
        assertThat(category1.options().get(1).optionValue()).isEqualTo("부산");

        // 두 번째 카테고리 검증 - 라벨 매핑 적용
        CategoryResponseDto category2 = result.get(1);
        assertThat(category2.categoryId()).isEqualTo(2L);
        assertThat(category2.categoryName()).isEqualTo("현재신분");
        assertThat(category2.options()).hasSize(1);

        assertThat(category2.options().get(0).optionValue()).isEqualTo("무소득(학생)");
    }

    @Test
    void 숨김_옵션_필터링_성공() {
        List<CategoryFlatDto> mockData = List.of(
                new CategoryFlatDto(3L, "저축기간", 1L, "3년 이상", "TERM_OVER_3_YEARS"),
                new CategoryFlatDto(3L, "저축기간", 2L, "1개월", "TERM_1_MONTH"),
                new CategoryFlatDto(3L, "저축기간", 3L, "6개월", "TERM_6_MONTH")
        );

        given(categoryRepository.findAllWithOptions()).willReturn(mockData);

        List<CategoryResponseDto> result = categoryService.getCategories();

        assertThat(result).hasSize(1);
        CategoryResponseDto category = result.get(0);
        assertThat(category.options()).hasSize(2);
        assertThat(category.options().get(0).optionValue()).isEqualTo("1개월");
        assertThat(category.options().get(1).optionValue()).isEqualTo("6개월");
    }

    @Test
    void 라벨_매핑_적용_성공() {
        List<CategoryFlatDto> mockData = List.of(
                new CategoryFlatDto(6L, "우대거래", 1L, "자동이체 가능", "BANK_AUTO_TRANSFER"),
                new CategoryFlatDto(6L, "우대거래", 2L, "마케팅 동의", "BANK_MARKETING"),
                new CategoryFlatDto(6L, "우대거래", 3L, "첫거래 고객", "BANK_FIRST_TRANSACTION")
        );

        given(categoryRepository.findAllWithOptions()).willReturn(mockData);

        List<CategoryResponseDto> result = categoryService.getCategories();

        assertThat(result).hasSize(1);
        CategoryResponseDto category = result.get(0);
        assertThat(category.options()).hasSize(2);
        assertThat(category.options().get(0).optionValue()).isEqualTo("자동이체 우대");
        assertThat(category.options().get(1).optionValue()).isEqualTo("마케팅동의 OK");
    }
}