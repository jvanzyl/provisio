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
package ca.vanzyl.provisio.model;

import java.util.Set;

//
// This should become Runtime and Runtime -> RuntimeDescriptor where
// The RuntimeDescriptor is used to materialized the Runtime
//
// Want to be able to produced a ResolvedRuntime after our provisioning
// process so we know exactly what's in the runtime
//
// So we can move between instances of Runtimes which may be different versions
// of the same product.
//
// Also want to be able to add to a runtime and possibly version it. Really I want
// something like a Git repo maybe I can leverage JGit.
//
public class ResolvedRuntime {

    private Set<ResolvedRuntimeElement> elements;
}
