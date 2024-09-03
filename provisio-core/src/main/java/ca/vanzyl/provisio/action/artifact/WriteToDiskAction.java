/*
 * Copyright (C) 2015-2024 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.action.artifact;

import static ca.vanzyl.provisio.ProvisioUtils.targetArtifactFileName;
import static java.util.Objects.requireNonNull;

import ca.vanzyl.provisio.ProvisioVariables;
import ca.vanzyl.provisio.ProvisioningException;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("write")
public class WriteToDiskAction implements ProvisioningAction {
    private static Logger logger = LoggerFactory.getLogger(WriteToDiskAction.class);

    private final ProvisioArtifact artifact;
    private final File outputDirectory;

    public WriteToDiskAction(ProvisioArtifact artifact, File outputDirectory) {
        requireNonNull(outputDirectory, "outputDirectory cannot be null.");
        this.artifact = artifact;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void execute(ProvisioningContext context) {
        if (artifact.getFile() != null) {
            write(
                    context,
                    artifact,
                    targetArtifactFileName(context, artifact, artifact.getFile().getName()));
        }
    }

    private void write(ProvisioningContext context, ProvisioArtifact source, String targetPath) {
        File target = new File(outputDirectory, targetPath);
        try {
            Files.createDirectories(target.getParentFile().toPath());
            if (ProvisioVariables.allowTargetOverwrite(context)) {
                if (Files.isRegularFile(target.toPath())) {
                    logger.warn("Conflict: artifact {} overwrote existing file {}", artifact, targetPath);
                }
                Files.copy(artifact.getFile().toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                if (Files.isRegularFile(target.toPath())) {
                    throw new ProvisioningException(
                            "Conflict: artifact " + artifact + " would overwrite existing file: " + targetPath);
                }
                logger.debug("Copying {} -> {}", artifact, targetPath);
                Files.copy(artifact.getFile().toPath(), target.toPath());
            }
        } catch (IOException e) {
            throw new ProvisioningException("Error copying " + source + " to " + targetPath, e);
        }
    }
}
