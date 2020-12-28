package user11681.plugin;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class Annotation extends HashMap<String, Object> {
    public final TypeElement type;
    public final Element annotatedElement;

    public Annotation(TypeElement type, Element annotatedElement, Map<String, Object> properties) {
        super(properties);

        this.type = type;
        this.annotatedElement = annotatedElement;
    }
}
