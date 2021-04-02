package user11681.plugin;

public class ResourceLocation {
    public final String packageName;
    public final String relativeName;

    public ResourceLocation(String path) {
        int separator = path.lastIndexOf('/');

        if (separator < 0) {
            this.packageName = "";
            this.relativeName = path;
        } else {
            this.packageName = path.substring(0, separator);
            this.relativeName = path.substring(separator + 1);
        }
    }

    public ResourceLocation(String packageName, String relativeName) {
        this.packageName = packageName;
        this.relativeName = relativeName;
    }
}
