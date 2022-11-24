package rules

import org.apache.commons.lang.StringUtils
import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule
import org.slf4j.LoggerFactory

class ProjectVersionPattern implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def logger = LoggerFactory.getLogger(getClass())
        def versionPattern = request.getValidationParameter("versionPattern")
        def errors = new ArrayList()

        if (versionPattern != null) {
            def tools = request.getTools()
            tools.paralleledInBatch(request.getSourcePaths(), { it ->
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    def vs = ref.getVersionString()
                    logger.info("Checking whether '{}' matches version-pattern: '{}' in rule-set: {}", vs, versionPattern, request.getRuleSet().getName())
                    
                    if (!vs.matches(versionPattern)) {
                        def msg = String.format("%s does not match version pattern: '%s' (version was: '%s')", it, versionPattern, vs)
                        logger.info(msg)
                        synchronized (errors) {
                            errors.add(msg)
                        }
                    }
                }
            })
        } else {
            logger.warn("No 'versionPattern' parameter specified in rule-set: {}. Cannot execute ProjectVersionPattern rule!", request.getRuleSet().getName())
        }

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
