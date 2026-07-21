# Trino streaming performance results, 2026-07-21

This run applies the protocol in [trino-performance-test.md](trino-performance-test.md)
to three worktrees based on Trino `9d29c8ae941`:

- **staged:** unchanged Provisio descriptors from the base commit;
- **custom:** Trino pull request 30400 commit `77080ed5caf`, replayed as
  `aa8603bdb3c` on the common base; and
- **streaming:** Provisio streaming commits through `f02e81393a3`, using
  Provisio `64493e6` and Provisio Archiver `32f07a8`.

The Archiver candidate avoids trimming compressed member buffers, uses fixed
1 MiB gzip members, and reduces archive-path bookkeeping. The Provisio
candidate exposes compression settings, and the Trino descriptors select
DEFLATE level 6.

## Environment

- macOS 15.7.3, AArch64
- Temurin 25.0.1+8
- Maven 3.9.16
- offline Maven execution after common snapshot installation
- Trino tests skipped

Every measured invocation used:

```shell
./mvnw -q -o \
  -pl core/trino-server-core,core/trino-server \
  -DskipTests \
  -Dair.check.skip-all \
  -Dmaven.source.skip=true \
  clean package
```

Each variant received one unreported warm-up. Seven measured rounds used the
rotating order documented in the benchmark design.

## Results

| Variant | Wall-clock samples, seconds | Median | MAD | Median user | Median system |
| --- | --- | ---: | ---: | ---: | ---: |
| staged | 50.23, 43.05, 43.67, 50.42, 49.01, 47.13, 46.89 | 47.13 | 3.10 | 44.92 | 12.29 |
| custom | 12.74, 10.94, 13.52, 10.87, 10.30, 11.62, 11.01 | 11.01 | 0.61 | 37.75 | 5.71 |
| streaming | 8.84, 7.48, 7.91, 9.10, 7.01, 7.95, 8.26 | 7.95 | 0.47 | 39.12 | 2.81 |

Streaming was 5.93 times faster than staged Provisio and 27.8% faster than the
same-base custom assembler. It was faster than the custom assembler in every
measured round.

| Variant | Combined archive size | Combined target footprint |
| --- | ---: | ---: |
| staged | 1,087,235,486 bytes (1,036.87 MiB) | 6.37 GiB |
| custom | 1,062,932,068 bytes (1,013.69 MiB) | 6.06 GiB |
| streaming | 1,063,503,312 bytes (1,014.24 MiB) | 1.00 GiB |

Streaming output was 571,244 bytes (0.054%) larger than the custom assembler
and 23,732,174 bytes (2.18%) smaller than staged Provisio.

## Correctness and reproducibility

- All six gzip streams passed complete validation.
- All six TAR streams enumerated completely: 1,090 core entries and 7,259
  server entries, including the root directory.
- The extracted streaming server tree matched the staged hard-link tree with
  no path or byte differences, and an independent filesystem manifest found
  no kind or mode differences.
- Repeated clean streaming builds produced identical SHA-256 values:
  - core: `72fd3e6aa7a58efd937b5e400113d949c351295cf3e8fa27df237b55f6ad9252`
  - server: `98a045350da902497cb1dff282eaa15609f48ce9031ff29c543375e3bf43b745`

Trino unit and integration tests were intentionally not run for this
macrobenchmark.

## Allocation diagnostic

Like-for-like JFR diagnostics before and after the Archiver changes showed:

| Metric | Before | After |
| --- | ---: | ---: |
| wall clock | 10.76 s | 8.24 s |
| maximum resident set | 2,984,034,304 bytes | 868,417,536 bytes |
| largest recorded heap before GC | about 3.1 GiB | 498.1 MiB |
| `Arrays.copyOf(byte[], int)` allocation pressure | 40.83% | 4.07% |

Maximum resident memory fell by about 70.9%. The remaining dominant Archiver
allocations are the bounded raw 1 MiB chunks and compressed-member buffers,
which are the intended working set. Buffer pooling was deliberately left out
of this change set.
