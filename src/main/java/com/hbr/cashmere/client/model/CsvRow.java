package com.hbr.cashmere.client.model;

public class CsvRow {
    private String coreProductId;
    private String availabilityPk;
    private String aiEligibilitySet;
    private String s3Path;

    public CsvRow(String coreProductId, String availabilityPk, String aiEligibilitySet, String s3Path) {
        this.coreProductId = coreProductId;
        this.availabilityPk = availabilityPk;
        this.aiEligibilitySet = aiEligibilitySet;
        this.s3Path = s3Path;
    }

    public String getCoreProductId() {
        return coreProductId;
    }

    public void setCoreProductId(String coreProductId) {
        this.coreProductId = coreProductId;
    }

    public String getAvailabilityPk() {
        return availabilityPk;
    }

    public void setAvailabilityPk(String availabilityPk) {
        this.availabilityPk = availabilityPk;
    }

    public String getAiEligibilitySet() {
        return aiEligibilitySet;
    }

    public void setAiEligibilitySet(String aiEligibilitySet) {
        this.aiEligibilitySet = aiEligibilitySet;
    }

    public String getS3Path() {
        return s3Path;
    }

    public void setS3Path(String s3Path) {
        this.s3Path = s3Path;
    }
}
