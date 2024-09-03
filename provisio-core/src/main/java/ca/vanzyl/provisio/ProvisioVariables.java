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
package ca.vanzyl.provisio;

import ca.vanzyl.provisio.model.ProvisioningContext;

/**
 * Knobs and switches to control Proviso behaviour.
 */
public final class ProvisioVariables {
    private ProvisioVariables() {}

    private static final String VARIABLE_PREFIX = "provisio.";

    /**
     * A boolean flag denoting that user allows "overwrite" to happen in case of conflicting target files. If set
     * to {@code true} Provisio will overwrite target files (but will warn). If set to {@code false} Provisio will
     * fail provisioning, if overwrite is about to happen. Defaults to {@code false}.
     * <p>
     * Note: Provisio older versions did silently overwrite conflicting target files. This change now makes Provisio
     * fail. If you need old behaviour, make this variable {@code true} and tolerate warnings. Ideally you want to
     * resolve this state.
     */
    public static final String ALLOW_TARGET_OVERWRITE = VARIABLE_PREFIX + "allowTargetOverwrite";

    /**
     * Option to control the "fallback" target file naming, used in cases when no explicit artifact name was given
     * (in XML using {@code artifact as="..."} construct). Originally Provisio used resolved artifact filename,
     * but it may lead to conflicts (same named file may exist under different groupId). The alternative option is
     * to "factor in" Artifact groupId into target file name, making it unique even in case of conflict.
     * Default value is {@link FallBackTargetFileNameMode#ARTIFACT_FILE_NAME}.
     *
     * @see #ALLOW_TARGET_OVERWRITE
     */
    public static final String FALLBACK_TARGET_FILE_NAME_MODE = VARIABLE_PREFIX + "fallbackTargetFileNameMode";

    /**
     * The enum values for {@link #FALLBACK_TARGET_FILE_NAME_MODE}.
     */
    public enum FallBackTargetFileNameMode {
        ARTIFACT_FILE_NAME,
        GA
    }

    /**
     * The separator to use between groupId and artifactId when in mode {@link FallBackTargetFileNameMode#GA},
     * defaults to underscore (@code "_").
     */
    public static final String GA_SEPARATOR = VARIABLE_PREFIX + "gaSeparator";

    /**
     * The maximum filename length to allow when in mode {@link FallBackTargetFileNameMode#GA},
     * defaults to 64.
     */
    public static final String GA_MAX_FILE_NAME_LENGTH = VARIABLE_PREFIX + "maxFileNameLength";

    //

    public static boolean allowTargetOverwrite(ProvisioningContext context) {
        return Boolean.parseBoolean(
                context.getVariables().getOrDefault(ALLOW_TARGET_OVERWRITE, Boolean.FALSE.toString()));
    }

    public static FallBackTargetFileNameMode fallbackTargetFileNameMode(ProvisioningContext context) {
        return FallBackTargetFileNameMode.valueOf(context.getVariables()
                .getOrDefault(FALLBACK_TARGET_FILE_NAME_MODE, FallBackTargetFileNameMode.ARTIFACT_FILE_NAME.name()));
    }

    public static String gaSeparator(ProvisioningContext context) {
        return context.getVariables().getOrDefault(GA_SEPARATOR, "_");
    }

    public static int gaMaxFileNameLength(ProvisioningContext context) {
        return Integer.parseInt(context.getVariables().getOrDefault(GA_MAX_FILE_NAME_LENGTH, "64"));
    }
}
