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
