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

import ca.vanzyl.provisio.action.artifact.ExcludeAction;
import ca.vanzyl.provisio.action.artifact.UnpackAction;
import ca.vanzyl.provisio.action.artifact.alter.AlterAction;
import ca.vanzyl.provisio.action.artifact.alter.Delete;
import ca.vanzyl.provisio.action.artifact.alter.Insert;
import ca.vanzyl.provisio.action.fileset.MakeExecutableAction;
import ca.vanzyl.provisio.action.runtime.ArchiveAction;
import ca.vanzyl.provisio.action.runtime.MakeDirectoryAction;
import ca.vanzyl.provisio.model.ActionDescriptor;
import ca.vanzyl.provisio.model.Alias;
import ca.vanzyl.provisio.model.Implicit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Actions {

    public static List<ActionDescriptor> defaultActionDescriptors() {
        List<ActionDescriptor> actionDescriptors = new ArrayList<>();
        actionDescriptors.add(new ActionDescriptor() {
            @Override
            public String getName() {
                return "unpack";
            }

            @Override
            public Class<?> getImplementation() {
                return UnpackAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {
                    "filter", "mustache", "includes", "excludes", "flatten", "useRoot", "dereferenceHardlinks"
                };
            }
        });

        actionDescriptors.add(new ActionDescriptor() {
            @Override
            public String getName() {
                return "exclude";
            }

            @Override
            public Class<?> getImplementation() {
                return ExcludeAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {"dir", "file"};
            }
        });

        actionDescriptors.add(new ActionDescriptor() {
            @Override
            public String getName() {
                return "archive";
            }

            @Override
            public Class<?> getImplementation() {
                return ArchiveAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {"name", "executable", "hardLinkIncludes", "hardLinkExcludes", "useRoot"};
            }
        });

        actionDescriptors.add(new ActionDescriptor() {
            @Override
            public String getName() {
                return "mkdir";
            }

            @Override
            public Class<?> getImplementation() {
                return MakeDirectoryAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {"name"};
            }
        });

        actionDescriptors.add(new ActionDescriptor() {
            @Override
            public String getName() {
                return "executable";
            }

            @Override
            public Class<?> getImplementation() {
                return MakeExecutableAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {"includes", "excludes"};
            }
        });

        actionDescriptors.add(new ActionDescriptor() {

            @Override
            public String getName() {
                return "alter";
            }

            @Override
            public Class<?> getImplementation() {
                return AlterAction.class;
            }

            @Override
            public String[] attributes() {
                return new String[] {};
            }

            @Override
            public List<Alias> aliases() {
                return Arrays.asList(new Alias("insert", Insert.class), new Alias("delete", Delete.class));
            }

            @Override
            public List<Implicit> implicits() {
                return Arrays.asList(
                        new Implicit("inserts", AlterAction.class, Insert.class),
                                new Implicit("artifacts", Insert.class),
                        new Implicit("deletes", AlterAction.class, Delete.class), new Implicit("files", Delete.class));
            }
        });
        return actionDescriptors;
    }
}
