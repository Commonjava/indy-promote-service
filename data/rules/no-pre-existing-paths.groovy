package rules

import org.apache.commons.lang.StringUtils
import org.commonjava.service.promote.validate.PromotionValidationException
import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule
import org.commonjava.service.promote.util.ContentDigest
import org.slf4j.LoggerFactory

class NoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def errors = new ArrayList()
        def tools = request.getTools()

        def logger = LoggerFactory.getLogger(ValidationRule.class)
        logger.info("Check pre-existing paths in: {}, paths: {}", Arrays.asList(verifyStoreKeys), request.getSourcePaths())

        tools.paralleledInBatch(request.getSourcePaths(), { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                String sourceChecksum = null
                tools.forEach(verifyStoreKeys, { verifyStoreKey ->
                    try {
                        if (tools.exists(verifyStoreKey, it)) {
                            logger.info("Found existing path, store: {}, path: {}", verifyStoreKey, it)
                            def sourceStoreKey = request.getPromoteRequest().getSource()
                            if (sourceChecksum == null) {
                                sourceChecksum = tools.digest(sourceStoreKey, it, ContentDigest.SHA_256)
                                logger.info("Digest source: {}, checksum: {}", sourceStoreKey, sourceChecksum)
                            }
                            String targetChecksum = tools.digest(verifyStoreKey, it, ContentDigest.SHA_256)
                            synchronized (errors) {
                                if (targetChecksum == null) {
                                    errors.add(String.format("failed to get checksum for %s in %s", it, verifyStoreKey))
                                } else if (!targetChecksum.equals(sourceChecksum)) {
                                    errors.add(String.format("%s is already available in %s with different checksum", it, verifyStoreKey))
                                }
                            }
                        } else {
                            logger.info("No existing path, store: {}, path: {}", verifyStoreKey, it)
                        }
                    } catch ( Exception e ) {
                        logger.error("Rule 'no-pre-existing-paths' failed", e)
                        errors.add("Rule 'no-pre-existing-paths' failed, error: " + e)
                    }
                })
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
