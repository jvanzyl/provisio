# Streaming assemblies

Provisio can assemble an archive directly from its inputs instead of first
materializing an exploded runtime directory. This is useful for distributions
containing many JARs, especially when the same JAR content appears in several
plugins.

Enable direct assembly on the runtime archive action:

```xml
<archive
  name="distribution.tar.gz"
  streaming="true"
  hardLinkIncludes="**/*.jar" />
```

`streaming` defaults to `false`, which retains the historical staged assembly
behavior. When direct assembly is eligible, Provisio creates
`distribution.tar.gz` but does not create the configured exploded runtime
directory. The resulting archive is still registered as the provisioning
result.

## Eligible inputs

A direct streaming assembly can contain:

- resolved artifacts copied as loose files;
- artifacts with exactly one `unpack` action, including standard filtering or
  Mustache transformation;
- ZIP-compatible inputs (`.zip`, `.jar`, `.war`, `.hpi`, and `.jpi`);
- gzip-compressed TAR inputs (`.tar.gz` and `.tgz`), including safe symbolic
  and hard links;
- resources;
- loose files from file sets; and
- file-set directories with their existing includes, excludes, and flattening
  selection.

Each source retains its own destination, root removal, selection, and flattening
rules. Archive paths and link targets are validated after mapping. Absolute
paths, root escapes, unsafe links, and mapped-path collisions fail the assembly.

## Staged fallbacks

Requesting streaming is an optimization request, not a change to the meaning of
an existing descriptor. Provisio logs the concrete reason and uses the staged
path when an assembly needs behavior that cannot yet be performed directly:

- filtering or Mustache transformation on a file-set directory;
- hard-link dereferencing while unpacking;
- multiple or otherwise unsupported artifact actions;
- touch files;
- a flattened file-set directory whose selected files have colliding names;
- another runtime action in addition to the archive action;
- `provisio.allowTargetOverwrite=true`, which requires historical last-writer
  semantics; or
- an unresolved artifact without a usable source file.

The staged path creates the exploded runtime directory as before. A build can
therefore adopt `streaming="true"` without silently dropping transformations or
changing overwrite behavior.

## Reproducibility and ordering

Streaming assemblies use normalized archive metadata and deterministic source
order. Normalization fixes timestamps and ownership fields, canonicalizes file
modes while retaining executable selection, and fixes gzip header fields.
Artifact entries and selected directory files are ordered deterministically;
ZIP-compatible sources sort their central-directory entries by name. TAR inputs
retain their existing source order, so reproducibility assumes that a TAR input
itself is stable.

Reproducibility does not require globally sorting or spooling all output
entries. The same inputs and descriptor produce byte-identical output, and
equivalent ZIP inputs do so even when their central-directory entry order
differs. This allows each entry to be consumed before its input source advances.

## CRC32 content identity

For TAR entries selected by `hardLinkIncludes`, Provisio uses uncompressed size
plus CRC32 when both values are supplied by the source. ZIP and JAR central
directories provide these values without opening and decompressing every
candidate entry. The first unique payload is read to write its regular TAR
entry; later entries with the same size and CRC32 can be emitted as hard links
without reading their compressed content.

CRC32 is an integrity checksum, not a collision-resistant digest. Two different
payloads can have the same size and CRC32. Provisio intentionally accepts that
risk for non-adversarial build inputs in exchange for avoiding the dominant
read, decompression, and SHA-256 work in large distributions. This mode must not
be treated as a security or authenticity check.

When a source does not provide usable size and CRC32 metadata, the archiver
falls back to verified SHA-256 identity. Loose files therefore remain
content-verified before becoming hard links. ZIP payloads that are actually
read are checked against their declared size and CRC32; a duplicate skipped
solely because its metadata matches is deliberately trusted rather than
independently validated.

## Failure and resource guarantees

The destination archive is written to a temporary sibling and moved into place
only after every source, output entry, compression worker, and trailer completes
successfully. A failed assembly preserves an existing destination and removes
its partial temporary output. Direct source-order assembly does not create an
entry-content sorting spool or an exploded runtime tree. Standard filtering and
Mustache transformation use one bounded temporary file for the entry currently
being transformed; it is removed before the source advances to the next entry.

## Future simplification

The current opt-in and staged fallback preserve existing Provisio descriptor
semantics while streaming coverage grows. They are not intended as a second
permanent assembly architecture. A future major refactor can make direct
assembly the normal path and remove the eligibility planner and staged archive
path after remaining transformations have streaming implementations, or after
their consumers have migrated. No Archiver 1 compatibility layer is retained
for that future refactor to remove.
