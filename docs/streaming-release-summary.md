# Streaming assembly release summary

This document tracks the coordinated Provisio Archiver 2.0.0 and Provisio
2.0.0 streaming-assembly work for release notes and the follow-up Trino pull
request update. Archiver 2.0.0 is published; Provisio 2.0.0 remains a release
target until its artifacts are published.

## Release order

1. Release `io.takari:takari-archiver:2.0.0`.
2. Replace Provisio's `2.0.0-SNAPSHOT` Archiver dependency with the released
   version, run the full Provisio test suite, and release Provisio 2.0.0.
3. Update Trino to the released Provisio version and rerun the focused server
   packaging correctness and reproducibility gates.
4. Add the final released versions and performance summary to Trino PR 30400.

No release, remote push, or pull-request operation is part of preparing these
notes.

## Provisio Archiver 2.0.0

Archiver 2 is a coordinated breaking release. It removes the old handler and
mutable-entry architecture instead of maintaining a compatibility layer.

### Architecture and API

- Sources use checked, callback-scoped traversal so sequential archive content
  is consumed before its source advances or closes.
- Per-operation `ArchiveSession` state prevents paths, entries, hard-link
  candidates, and temporary content from leaking across invocations.
- Source entries, entry content, immutable output entries, and format writers
  have separate responsibilities.
- Mapping is configured per source through `SourceSpec`.
- `Path` is the primary filesystem API, and `Sources` is the supported public
  source facade.
- Source order and name order are explicit and independent from metadata
  normalization.
- Obsolete handlers, mutable entry APIs, `File` overloads, artifact generators,
  permission internals, and other implementation-facing entry points were
  removed from the public contract.
- XZ support, fixtures, tests, and historical XZ/gzip combined class names were
  removed. TAR.GZ input and output now use clearly named reader/source and
  writer APIs.

### Correctness and reproducibility

- Archive paths, mapped paths, and links are validated against absolute paths,
  traversal, platform-specific root forms, unsafe link targets, and collisions.
- ZIP size and CRC32 and TAR header checksums are validated when content is
  consumed; corrupt input fails transactionally.
- Output is written to a temporary sibling and moved into place only after
  sources, writers, compression workers, and trailers complete successfully.
- Normalized metadata and deterministic source ordering preserve byte-for-byte
  reproducibility without requiring a global sorting spool.
- Hard links use verified content identity by default. Explicit size-plus-CRC32
  identity supports trusted build inputs and can avoid reading duplicate ZIP
  payloads. Cross-source identity permits loose files, TAR, and ZIP inputs to
  share hard-link targets.

### Performance

- Bounded parallel gzip emits fixed 1 MiB members in submission order, keeping
  output identical across worker counts.
- Compressed-member buffer copies were removed.
- Destination-prefix parsing and parent-path discovery were reduced.
- Source-order assembly avoids an exploded runtime tree and archive-sized entry
  spool.

The complete Archiver notes live in its
`docs/release-notes-2.0.0.md` and `docs/streaming-architecture.md` files.

## Provisio 2.0.0

- Provisio is migrated to the Archiver 2 API.
- Runtime archive actions accept `streaming="true"` as an opt-in optimization.
- Eligible loose artifacts, ZIP/JAR/TAR.GZ artifact contents, resources, loose
  file-set files, and selected file-set directories flow directly into the
  output archive.
- Per-source destination, root removal, includes, excludes, flattening, file
  mode selection, standard filtering, and Mustache transformation are retained.
- Unsupported combinations log a concrete reason and fall back to the existing
  staged implementation, preserving descriptor meaning.
- Direct assemblies use normalized metadata, deterministic source order, and
  cross-source content identity. They retain reproducibility without sorting or
  spooling every output entry.
- `gzipCompressionLevel` exposes DEFLATE levels `-1` through `9`, and
  `gzipCompressionThreads` exposes a bounded worker count from `1` through
  `256`. Trino selects level 6 after comparing throughput and archive size.
- Archive extraction and streaming failure handling preserve existing outputs
  and clean partial work.

The eligibility and fallback contract is documented in
[Streaming assemblies](streaming-assemblies.md).

## Test and correctness evidence

- Provisio Archiver: 143 tests pass.
- Provisio: 117 tests pass with zero failures or errors and one pre-existing
  skipped test.
- Negative coverage includes malformed paths and links, collisions, corrupt
  archives, callback-lifetime misuse, transactional write failures, compression
  worker/output failure, cancellation, interruption, unsupported streaming
  plans, extraction failure, and fallback boundaries.
- All six benchmark gzip and TAR streams validated completely.
- The extracted streaming Trino server matched staged output byte-for-byte,
  including filesystem entry kinds and modes.
- Repeated streaming builds produced identical core and server archives.

## Trino performance evidence

The benchmark used a common Trino base and equivalent offline Maven
repositories. Plugin artifacts and the common reactor dependency closure were
built before measurement. Each measured invocation ran `clean package` only for
`core/trino-server-core` and `core/trino-server`, with tests and Airlift checks
skipped. The results therefore isolate final server distribution packaging;
they are not complete clean Trino reactor build times.

| Variant | Median | MAD | Combined target footprint |
| --- | ---: | ---: | ---: |
| Staged Provisio | 47.13 s | 3.10 s | 6.37 GiB |
| Trino PR 30400 custom assembler | 11.01 s | 0.61 s | 6.06 GiB |
| Streaming Provisio | 7.95 s | 0.47 s | 1.00 GiB |

Streaming Provisio was 27.8% faster than the same-base custom assembler and
5.93 times faster than staged Provisio. It won all seven measured rounds against
the custom assembler. Its combined archives were 0.054% larger than the custom
assembler and 2.18% smaller than staged Provisio.

Like-for-like JFR profiling across the final Archiver optimizations reduced
wall time from 10.76 seconds to 8.24 seconds, maximum resident memory from about
2.98 GB to 868 MB, largest heap before GC from about 3.1 GiB to 498 MiB, and
`Arrays.copyOf(byte[], int)` allocation pressure from 40.83% to 4.07%.

The full protocol and raw samples are in [Trino performance test](trino-performance-test.md)
and [Trino performance results](trino-performance-results-2026-07-21.md).

## Trino PR note draft

The following can be adapted after both artifacts are released:

> This revision replaces the PR-specific server assembler with Provisio
> [PROVISIO_VERSION], backed by Provisio Archiver [ARCHIVER_VERSION]. Provisio
> now streams eligible artifacts and file sets directly into a reproducible
> TAR.GZ without materializing the exploded server tree. It preserves normalized
> metadata and deterministic source order, validates mapped paths and links,
> writes transactionally, and uses trusted size-plus-CRC32 identity for
> non-adversarial build inputs to emit duplicate JARs as hard links without
> decompressing them again. Bounded parallel gzip uses deterministic 1 MiB
> members; Trino selects compression level 6.
>
> On a common Trino base, after building the plugin artifacts and common reactor
> dependency closure, seven measured `clean package` rounds for
> `core/trino-server-core` and `core/trino-server` produced medians of 47.13 s
> for staged Provisio, 11.01 s for the PR-specific assembler, and 7.95 s for
> streaming Provisio. Streaming Provisio was 27.8% faster than the custom
> assembler, used a 1.00 GiB target footprint instead of 6.06 GiB, and produced
> archives only 0.054% larger. The extracted server matched staged output by
> path, bytes, kind, and mode, and repeated streaming builds were byte-identical.
> These timings cover the two final server packaging modules, not a complete
> clean Trino reactor build.
