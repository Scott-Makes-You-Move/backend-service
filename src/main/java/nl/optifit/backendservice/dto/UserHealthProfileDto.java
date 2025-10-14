package nl.optifit.backendservice.dto;

public record UserHealthProfileDto(String sex, int age, double weight, double fat, int visceralFat) {
}
