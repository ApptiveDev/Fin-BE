package apptive.fin.search.controller;

import apptive.fin.search.dto.DynamicFormResponseDto;
import apptive.fin.search.dto.ProductNameSearchDto;
import apptive.fin.search.dto.ProductSearchResultDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.service.DynamicFormService;
import apptive.fin.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final DynamicFormService dynamicFormService;
    private final SearchService searchService;

    @PostMapping("/dynamic-form")
    public DynamicFormResponseDto dynamicForm(@Valid @RequestBody SearchRequestDto searchRequestDto) {
        return dynamicFormService.calcFormCondition(searchRequestDto);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductSearchResultDto> search(@Valid @RequestBody SearchRequestDto searchRequestDto) {
        return ResponseEntity.ok(searchService.search(searchRequestDto));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductNameSearchDto>> searchByName(
            @RequestParam String searchInput
    ){
        return ResponseEntity.ok(searchService.searchByName(searchInput));
    }

}
