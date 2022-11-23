package rules

import org.apache.commons.lang.StringUtils
import org.commonjava.service.promote.validate.PromotionValidationException
import org.commonjava.service.promote.validate.ValidationRequest
import org.commonjava.service.promote.validate.ValidationRule

class NPMNoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def errors = Collections.synchronizedList(new ArrayList());
        def tools = request.getTools()

        tools.paralleledEach(request.getSourcePaths(), { it ->
            tools.forEach(verifyStoreKeys, { verifyStoreKey ->
                if (tools.exists(verifyStoreKey, it)) {
                    errors.add(String.format("%s is already available in: %s", it, verifyStoreKey))
                }
            })
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
