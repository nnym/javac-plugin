package user11681.plugin;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

@SuppressWarnings("SameParameterValue")
public abstract class TransformingCompilerPlugin extends AbstractCompilerPlugin {
    protected static final HashMap<String, ClassNode> classCache = new HashMap<>();

    public TransformingCompilerPlugin(String name) {
        super(name);
    }

    /**
     * @return whether to overwrite the file containing the class.
     */
    protected abstract boolean transformClass(ClassNode klass) throws Throwable;

    @Override
    protected void done() throws Throwable {
        Set<JavaFileObject.Kind> classFiles = new HashSet<>();
        classFiles.add(JavaFileObject.Kind.CLASS);

        for (JavaFileObject file : this.files.list(StandardLocation.CLASS_OUTPUT, "", classFiles, true)) {
            ClassNode klass = new ClassNode();
            new ClassReader(file.openInputStream()).accept(klass, 0);

            if (this.transformClass(klass)) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                klass.accept(writer);

                OutputStream output = this.getClassOutput(klass.name);
                output.write(writer.toByteArray());
                output.close();
            }
        }
    }

    protected ClassNode getClass(String name) throws Throwable {
        return this.getClass(name, 0);
    }

    protected ClassNode getClass(String name, int parsingOptions) throws Throwable {
        ClassNode klass = classCache.get(name);

        if (klass != null) {
            return klass;
        }

        klass = new ClassNode();
        new ClassReader(this.getInputClass(name).openInputStream()).accept(klass, parsingOptions);

        classCache.put(name, klass);

        return klass;
    }
}
