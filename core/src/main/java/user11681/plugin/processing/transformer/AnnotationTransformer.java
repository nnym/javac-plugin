package user11681.plugin.processing.transformer;

import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import user11681.plugin.processing.AnnotationContainer;
import user11681.plugin.processing.ExpansionOptions;

public class AnnotationTransformer implements Transformer {
    public static final AnnotationTransformer instance = new AnnotationTransformer();

    @Override
    public Optional<Object> transform(ExpansionOptions options, Object value) {
        if (value instanceof AnnotationMirror mirror) {
            AnnotationContainer annotation = new AnnotationContainer(null, mirror);

            return Optional.of(options.shouldSerialize() ? annotation.elements(options) : annotation);
        }

        return Optional.empty();
    }
}
