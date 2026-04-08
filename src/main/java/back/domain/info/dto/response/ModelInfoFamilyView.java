package back.domain.info.dto.response;

import back.domain.info.entity.AiModelFamily;

public record ModelInfoFamilyView(
        String vendorName, String familyName, String commonDescription, String[] inputTypes, String[] outputTypes) {

    public ModelInfoFamilyView {
        inputTypes = inputTypes == null ? null : inputTypes.clone();
        outputTypes = outputTypes == null ? null : outputTypes.clone();
    }

    public static ModelInfoFamilyView from(AiModelFamily family) {
        return new ModelInfoFamilyView(
                family.getVendor().getName(),
                family.getFamilyName(),
                family.getCommonDescription(),
                family.getInputTypes(),
                family.getOutputTypes());
    }

    @Override
    public String[] inputTypes() {
        return inputTypes == null ? null : inputTypes.clone();
    }

    @Override
    public String[] outputTypes() {
        return outputTypes == null ? null : outputTypes.clone();
    }
}
