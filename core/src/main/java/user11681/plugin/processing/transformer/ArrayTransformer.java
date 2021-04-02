package user11681.plugin.processing.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.AnnotationValue;
import user11681.plugin.processing.AnnotationContainer;
import user11681.plugin.processing.ExpansionOptions;

@SuppressWarnings("unchecked")
public class ArrayTransformer implements Transformer {
    public static final ArrayTransformer instance = new ArrayTransformer();

    @Override
    public Optional<Object> transform(ExpansionOptions options, Object value) {
        if (value instanceof List) {
            List<AnnotationValue> values = (List<AnnotationValue>) value;
            List<Object> newValues = new ArrayList<>(values.size());

            for (AnnotationValue item : values) {
                newValues.add(AnnotationContainer.getValue(options, item));
            }

            return Optional.of(newValues);
        }

        return Optional.empty();
    }
}
