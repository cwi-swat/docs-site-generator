import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.lucene.index.IndexUpgrader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

public class UpgradeIndexes {
    public static void main(String[] args) throws IOException {
        Path sourcePath = new File(args[0]).toPath();
        System.out.println("Upgrading indexes in: " + sourcePath);
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".cfe")) {
                    FSDirectory dir = FSDirectory.open(file.getParent());
                    try {
                        new IndexUpgrader(dir, new IndexWriterConfig(null), true).upgrade();
                    } catch (IOException e) {
                        System.err.println("!! Could not update index: " + file.getParent() + "(" + e.getMessage() + ")");
                    }
                }
                return super.visitFile(file, attrs);
            }
        });
    }

}