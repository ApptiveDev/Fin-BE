package apptive.fin.category;

import apptive.fin.category.dto.CategoryResponseDto;
import apptive.fin.category.dto.OptionDto;
import apptive.fin.category.service.CategoryService;
import apptive.fin.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway migration(V2 기준 데이터, V9 저축기간, V11 문구 변경)을 로드한 실제 DB에서
 * STEP1 카테고리 응답이 기획 문구와 숨김 처리대로 나가는지 확인한다.
 */
class CategoryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryService categoryService;

    @Test
    void STEP1_카테고리는_기획_문구로_나가고_숨김_항목은_빠진다() {
        List<CategoryResponseDto> result = categoryService.getCategories();

        assertThat(result).extracting(CategoryResponseDto::categoryName)
                .containsExactly("거주지역", "현재신분", "저축기간", "핵심 혜택", "은행 우대 조건");

        assertThat(optionValues(result, 2L)).contains("미취업", "알바/프리랜서");
        assertThat(optionValues(result, 3L))
                .containsExactly("1개월", "3개월", "6개월", "1년", "2년", "3년");
        assertThat(optionValues(result, 4L)).contains("최고이율 중심");
        assertThat(optionValues(result, 6L))
                .containsExactlyInAnyOrder("자동이체 우대", "카드실적 우대", "급여이체 우대", "마케팅동의 OK");
    }

    private List<String> optionValues(List<CategoryResponseDto> categories, Long categoryId) {
        return categories.stream()
                .filter(c -> c.categoryId().equals(categoryId))
                .findFirst()
                .orElseThrow()
                .options().stream()
                .map(OptionDto::optionValue)
                .toList();
    }
}
