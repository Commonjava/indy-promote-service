package rules

import org.apache.commons.lang.StringUtils
import org.commonjava.service.promote.validate.PromotionValidationException
import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule
import org.commonjava.service.promote.util.ContentDigest

class NoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def errors = new ArrayList()
        def tools = request.getTools()

        tools.paralleledInBatch(request.getSourcePaths(), { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                // String sourceChecksum = null;
                tools.forEach(verifyStoreKeys, { verifyStoreKey ->
                    if (tools.exists(verifyStoreKey, it)) {
                        // if (sourceChecksum == null) {
                        //     sourceChecksum = tools.digest(request.getPromoteRequest().getSource(), it, ContentDigest.SHA_256);
                        // }
                        // if ( !tools.digest(verifyStoreKey, it, ContentDigest.SHA_256).equals(sourceChecksum)) {
                        synchronized (errors) {
                            errors.add(String.format("%s is already available with different checksum in: %s", it, verifyStoreKey))
                        }
                        // }
                    }
                })
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
