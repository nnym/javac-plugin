package user11681.plugin.processing.transformer;

import java.util.Optional;
import javax.lang.model.type.DeclaredType;
import user11681.plugin.ElementUtil;
import user11681.plugin.processing.ExpansionOptions;

public class TypeTransformer implements Transformer {
    public static final TypeTransformer instance = new TypeTransformer();

    @Override
    public Optional<Object> transform(ExpansionOptions options, Object value) {
        if (options.shouldSerialize() && value instanceof DeclaredType) {
            return Optional.of(ElementUtil.name((DeclaredType) value));
        }

        return Optional.empty();
    }
}
