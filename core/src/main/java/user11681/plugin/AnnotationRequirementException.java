package user11681.plugin;

import java.util.Arrays;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class AnnotationRequirementException extends RuntimeException {
    public AnnotationRequirementException(String message) {
        super(message);
    }

    public static AnnotationRequirementException incompatible(TypeElement first, TypeElement second, Element annotatedElement) {
        return new AnnotationRequirementException(String.format(
            "annotation of type %s is incompatible with %s on %s.",
            first.getQualifiedName(),
            second.getQualifiedName(),
            annotatedElement instanceof TypeElement
                ? ((TypeElement) annotatedElement).getQualifiedName()
                : String.format(
                    "member %s declared in %s",
                    annotatedElement,
                    annotatedElement.getEnclosingElement() instanceof TypeElement
                        ? ElementUtil.name(annotatedElement.getEnclosingElement())
                        : String.format(
                            "declared in %s",
                            ElementUtil.name(annotatedElement.getEnclosingElement().getEnclosingElement())
                        )
                )
        ));
    }

    public static AnnotationRequirementException required(TypeElement first, String[] missing, Element annotatedElement) {
        return new AnnotationRequirementException(String.format(
            "annotation of type %s on %s requires annotations of the following types: %s.",
            first.getQualifiedName(),
            Arrays.toString(missing),
            annotatedElement instanceof TypeElement
                ? ((TypeElement) annotatedElement).getQualifiedName()
                : String.format(
                    "member %s declared in %s",
                    annotatedElement,
                    annotatedElement.getEnclosingElement() instanceof TypeElement
                        ? ElementUtil.name(annotatedElement.getEnclosingElement())
                        : String.format(
                            "declared in %s",
                            ElementUtil.name(annotatedElement.getEnclosingElement().getEnclosingElement())
                        )
                )
        ));
    }
}
