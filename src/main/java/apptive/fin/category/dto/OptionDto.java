package apptive.fin.category.dto;

public record OptionDto(Long optionId, String label, String value) {
    public static OptionDto from(Long optionId, String value) {
        return new OptionDto(optionId, value, value);
    }
}
