package models;

public class ProductCriteria {
    private String nameContains;
    private String descriptionContains;
    private String manufacturer;
    private String manufacturerContains;
    private Double priceFrom;
    private Double priceTill;
    private Double quantityFrom;
    private Double quantityTill;

    public ProductCriteria() {
    }

    public ProductCriteria(String nameContains, String descriptionContains, String manufacturer,
                           String manufacturerContains, Double priceFrom, Double priceTill, Double quantityFrom,
                           Double quantityTill) {
        this.nameContains = nameContains;
        this.descriptionContains = descriptionContains;
        this.manufacturer = manufacturer;
        this.manufacturerContains = manufacturerContains;
        this.priceFrom = priceFrom;
        this.priceTill = priceTill;
        this.quantityFrom = quantityFrom;
        this.quantityTill = quantityTill;
    }

    public String getNameContains() {
        return nameContains;
    }

    public ProductCriteria setNameContains(String nameContains) {
        this.nameContains = nameContains;
        return this;
    }

    public String getDescriptionContains() {
        return descriptionContains;
    }

    public ProductCriteria setDescriptionContains(String descriptionContains) {
        this.descriptionContains = descriptionContains;
        return this;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public ProductCriteria setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }

    public String getManufacturerContains() {
        return manufacturerContains;
    }

    public ProductCriteria setManufacturerContains(String manufacturerContains) {
        this.manufacturerContains = manufacturerContains;
        return this;
    }

    public Double getPriceFrom() {
        return priceFrom;
    }

    public ProductCriteria setPriceFrom(Double priceFrom) {
        this.priceFrom = priceFrom;
        return this;
    }

    public Double getPriceTill() {
        return priceTill;
    }

    public ProductCriteria setPriceTill(Double priceTill) {
        this.priceTill = priceTill;
        return this;
    }

    public Double getQuantityFrom() {
        return quantityFrom;
    }

    public ProductCriteria setQuantityFrom(Double quantityFrom) {
        this.quantityFrom = quantityFrom;
        return this;
    }

    public Double getQuantityTill() {
        return quantityTill;
    }

    public ProductCriteria setQuantityTill(Double quantityTill) {
        this.quantityTill = quantityTill;
        return this;
    }
}
