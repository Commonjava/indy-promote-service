package rules

import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory

class ParsablePom implements ValidationRule {

    String validate(ValidationRequest request) {
        def errors = new ArrayList()
        def tools = request.getTools()
        def logger = LoggerFactory.getLogger(ValidationRule.class)
        logger.info("Parsing POMs in:\n  {}.", request.getSourcePaths().join("\n  "))

        tools.paralleledInBatch(request.getSourcePaths(), { it ->
            if (it.endsWith(".pom")) {
                logger.info("Parsing POM from path: {}.", it)
                try {
                    tools.readLocalPom(it, request)
                }
                catch (Exception e) {
                    synchronized(errors) {
                        errors.add(String.format("%s: Failed to parse POM. Error was: %s", it, e))
                    }
                }
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}