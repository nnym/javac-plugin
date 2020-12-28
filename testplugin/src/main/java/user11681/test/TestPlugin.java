package user11681.test;

import org.objectweb.asm.tree.ClassNode;
import user11681.plugin.Annotation;
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
    protected void processAnnotation(Annotation annotation) throws Throwable {
        super.processAnnotation(annotation);
    }
}
