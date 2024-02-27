package ca.vanzyl.provisio.archive;

import ca.vanzyl.provisio.archive.zip.ZipArchiveSource;
import java.io.File;
import java.io.IOException;

public class ZipArchiveValidator extends AbstractArchiveValidator {

    public ZipArchiveValidator(File archive) throws IOException {
        super(new ZipArchiveSource(archive));
    }
}
