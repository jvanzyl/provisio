package ca.vanzyl.provisio.archive;

import java.io.File;
import java.io.IOException;

public class ZipArchiveValidator extends AbstractArchiveValidator {

    public ZipArchiveValidator(File archive) throws IOException {
        super(Sources.zip(archive.toPath()));
    }
}
