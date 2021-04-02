package user11681.test;

import javax.lang.model.element.Element;
import org.objectweb.asm.tree.ClassNode;
import user11681.plugin.processing.AnnotationContainer;
import user11681.plugin.TransformingCompilerPlugin;

public class TestPlugin extends TransformingCompilerPlugin {
    protected TestPlugin() {
        super("test");
    }

    @Override
    protected boolean transformClass(ClassNode klass) throws Throwable {
        return false;
    }

    @Override
    protected void processAnnotation(Element element, AnnotationContainer annotation) throws Throwable {
        super.processAnnotation(element, annotation);
    }
}
