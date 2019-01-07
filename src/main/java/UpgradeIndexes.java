import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexUpgrader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class UpgradeIndexes {
    public static void main(String[] args) throws IOException {
        Path sourcePath = new File(args[0]).toPath();
        System.out.println("Reading indexes from: " + sourcePath);
        List<Directory> indexes = new ArrayList<>();
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".si")) {
                    FSDirectory dir = FSDirectory.open(file.getParent());
                    // try to open it to see if it is corrupt
                    try (DirectoryReader ireader = DirectoryReader.open(dir)) {
                        indexes.add(dir);
                    } catch (IOException e) {
                        System.err.println("!! Corrupt index found: " + file.getParent() + "(" + e.getMessage() + ")");
                    }
                }
                return super.visitFile(file, attrs);
            }
        });
        System.out.println("Upgrading " + indexes.size() + " indexes");
        for (Directory ix : indexes) {
            new IndexUpgrader(ix, new IndexWriterConfig(null), true).upgrade();
            ix.close();
        }
    }

}