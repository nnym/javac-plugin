package user11681.plugin.processing.transformer;

import java.util.Optional;
import jdk.jfr.Experimental;
import user11681.plugin.processing.ExpansionOptions;

@Experimental
public interface Transformer {
    Optional<Object> transform(ExpansionOptions options, Object value);
}
