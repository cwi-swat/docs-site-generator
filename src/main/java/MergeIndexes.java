import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.HardlinkCopyDirectoryWrapper;

class MergeIndexes {
    public static void main(String[] args) throws IOException {
        // based on the IndexMergeTool

        Path sourcePath = new File(args[1]).toPath();
        System.out.println("Reading indexes from: " + sourcePath);
        List<Directory> indexes = new ArrayList<>();
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".cfe")) {
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

        System.out.println("Merging " + indexes.size() + " indexes");
        for (Directory p : indexes) {
            System.out.println("\t" + p);
        }
        try (Directory mergedIndex = new HardlinkCopyDirectoryWrapper(FSDirectory.open(Paths.get(args[0])))) {
            try (IndexWriter writer = new IndexWriter(mergedIndex, new IndexWriterConfig(null).setOpenMode(OpenMode.CREATE))) {
                writer.addIndexes(indexes.toArray(new Directory[0]));
            }
        }
        System.out.println("Done merging");
        indexes.forEach(i -> {
            try {
                i.close();
            } catch (IOException e) {
            }
        });
    }
}