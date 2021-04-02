package user11681.plugin.processing.transformer;

import java.util.Optional;
import javax.lang.model.element.VariableElement;
import user11681.plugin.processing.ExpansionOptions;

public class EnumTransformer implements Transformer {
    public static final EnumTransformer instance = new EnumTransformer();

    @Override
    public Optional<Object> transform(ExpansionOptions options, Object value) {
        return value instanceof VariableElement ? Optional.of(value.toString()) : Optional.empty();
    }
}
