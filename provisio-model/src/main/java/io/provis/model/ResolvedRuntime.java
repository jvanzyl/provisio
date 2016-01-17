/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

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
