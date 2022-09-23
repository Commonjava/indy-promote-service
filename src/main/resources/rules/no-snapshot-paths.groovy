package rules

import org.apache.commons.lang.StringUtils
import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig

class NoSnapshots implements ValidationRule {

    String validate(ValidationRequest request) {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, true)

        def errors = new ArrayList()
        def tools = request.getTools()

        tools.paralleledInBatch(request.getSourcePaths(), { it ->
            if (it.endsWith(".pom")) {
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    if (!ref.getVersionSpec().isRelease()) {
                        synchronized (errors) {
                            errors.add(String.format("%s is a variable/snapshot version.", it))
                        }
                    }
                }
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}