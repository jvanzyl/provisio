# Trino packaging performance-test design

This benchmark validates that Provisio's streaming assembly retains the build
performance, output semantics, and reproducibility demonstrated by Trino pull
request 30400. It is a macrobenchmark, not a timing assertion in the ordinary
unit-test suite.

## Comparands

Every variant must start from the same Trino commit and consume the same built
dependency artifacts. Comparing the pull-request head directly with a newer
Trino `master` is useful for exploration but is not a controlled performance
test because dependency upgrades can change both input bytes and duplicate
content.

The harness creates three worktrees from one pinned Trino base:

1. **staged** retains the released Provisio configuration;
2. **custom** applies the server-assembler change from Trino pull request 30400
   to that base; and
3. **streaming** changes only the Provisio version and enables
   `streaming="true"` in the two server descriptors.

The exact Trino base SHA, Provisio and Archiver SHAs, custom-assembler patch
SHA, JDK, Maven version, operating system, architecture, CPU count, and
filesystem are recorded with every result.

## Preparation

Build and install the common Trino dependency closure once. Snapshot that local
Maven repository state, then give each variant an equivalent copy so one run
cannot overwrite another variant's snapshot artifacts. Install the candidate
Archiver and Provisio snapshots into the streaming variant's repository and the
custom assembler into the custom variant's repository. All measured builds run
offline.

The measured command packages only the two distribution modules and never runs
Trino tests:

```shell
./mvnw -q -o \
  -pl core/trino-server-core,core/trino-server \
  -DskipTests \
  -Dair.check.skip-all \
  -Dmaven.source.skip=true \
  clean package
```

Preparation time, dependency downloads, snapshot installation, and the common
reactor build are excluded from the measurement.

## Correctness gate

Timing results are invalid unless each variant first passes its correctness
gate. The verifier must:

* require streaming and staged to have the same relative path set;
* extract each archive and compare regular-file bytes, executable modes,
  directory modes, symbolic-link targets, and effective hard-link contents;
* compare the streaming output with the staged output from the same Trino base;
* compare custom with staged using exact paths outside `lib/` and a multiset of
  file contents, modes, and link semantics inside `lib/`, where pull request
  30400 intentionally changes JAR naming from underscores to dots;
* build each variant twice and require byte-identical output within that
  variant;
* validate the gzip and TAR streams completely;
* report regular-file, hard-link, directory, and symbolic-link counts;
* report archive sizes and the total package-module target footprint; and
* require the streaming variant to leave no exploded distribution, hard-link
  expansion, archive-entry spool, or artifact staging tree.

Different hard-link target choices between implementations are allowed when
the extracted filesystem semantics match. Compressed archives from different
implementations are not required to be byte-identical.

## Measurement protocol

Run one unreported warm-up for each variant. Then run at least seven measured
rounds in rotating order so cache warmth and machine drift are distributed:

```text
round 1: staged, custom, streaming
round 2: custom, streaming, staged
round 3: streaming, staged, custom
```

Repeat that rotation until every variant has the same sample count. The harness
removes only the two package-module target directories before each invocation;
it does not purge the prepared dependency repository.

Wall-clock duration is the primary metric. Also record process user and system
CPU time, maximum resident memory when the platform exposes it, archive sizes,
target footprint, and exit status. Preserve every raw sample in JSON and emit a
human-readable summary containing the median and median absolute deviation.
Do not discard slow samples after measurement; a run affected by a known
external interruption invalidates and repeats the whole round.

## Acceptance criteria

Correctness criteria are absolute. Performance criteria are relative because
absolute seconds vary substantially between machines:

* streaming has zero correctness or reproducibility differences from staged;
* streaming creates no expanded or staged payload tree;
* streaming's median package time is no more than 15% slower than the custom
  assembler;
* streaming is at least three times faster than staged Provisio; and
* streaming archives are no larger than the staged archives.

The 15% custom-assembler margin detects a material regression while allowing
normal filesystem and JVM noise. The three-times baseline threshold leaves
substantial headroom below the approximately five-times improvement observed
during development.

## Test layers

The real Trino benchmark is opt-in and should run manually or on dedicated
performance workers, not on every pull request. Stable CI should instead assert
structural properties: cross-source CRC32 deduplication, bounded buffering,
transactional failure, deterministic output, and absence of an exploded tree.
Those properties belong in Provisio and Provisio Archiver tests and must fail
normally if the streaming design regresses.

The benchmark harness should live with Provisio and accept the three Trino
worktree paths as arguments. Keeping orchestration outside Trino avoids adding
a permanent three-implementation benchmark to the consumer repository while
still testing the real Trino descriptors and dependency graph.
