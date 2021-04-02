package user11681.plugin;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public class ElementUtil {
    public static String name(DeclaredType type) {
        return name(type.asElement());
    }

    public static String name(Element element) {
        return (element instanceof TypeElement ? ((TypeElement) element).getQualifiedName() : element.getSimpleName()).toString();
    }
}
