# Aho–Corasick Multi-Pattern Search (Java)

**Author:** Abylai Duisim, group SE-2530

Implementation of the Aho–Corasick algorithm in Java and an experimental comparison with running KMP separately for each pattern.

## Project structure

```
src/AhoCorasick.java   trie + failure links + output links + search
src/KMP.java           prefix function + single-pattern search (baseline)
src/Tests.java         correctness tests
src/Benchmark.java     experiments: 10/100/1000 patterns x 1k/10k/100k text, 5 runs averaged
results.csv            raw results produced by Benchmark
```

## How to run

```
javac -d out src/*.java
java -cp out Tests
java -cp out Benchmark results.csv
```

Or open the project in IntelliJ IDEA and run `Tests` and `Benchmark`.

## Implementation

1. **Trie construction.** All patterns are inserted character by character into one trie. Every node has 256 transitions (ASCII / Latin-1). A node where a pattern ends stores the id of that pattern (duplicate patterns are allowed).
2. **Failure links (BFS).** `fail[v]` is the node for the longest proper suffix of the string of `v` that is also a prefix of some pattern. For a child `v = next[u][c]`, `fail[v] = next[fail[u]][c]`. Missing transitions are filled with `next[u][c] = next[fail[u]][c]`, so the search never has to walk along failure links in a loop.
3. **Output links.** `out[v]` points to the nearest node on the failure chain that is the end of a pattern. With them, patterns that are suffixes of other patterns (for example `he` inside `she`) are reported without walking the whole failure chain.
4. **Search.** The text is read once from left to right: `node = next[node][c]`. After each character the current node and all nodes reachable through output links are reported. All occurrences, including overlapping ones, are found.

## Tests

Correctness is checked on:

- the classic example (`he`, `she`, `his`, `hers` in `ushers`);
- nested and overlapping patterns (`a`, `aa`, `aaa` in `aaaa`);
- patterns that are suffixes of each other (`abcd`, `bcd`, `cd`, `d`);
- duplicate patterns, empty text, pattern longer than the text, no matches;
- random tests: the result of Aho–Corasick is compared with a brute-force search and with KMP.

## Experiments

**Setup**

- Number of patterns: 10, 100, 1,000. Text length: 1,000, 10,000, 100,000 characters.
- Random text and patterns over the alphabet `a–d`, pattern length 4–8, fixed random seed.
- Time is measured with `System.nanoTime()`. Each experiment is run **5 times and the average is reported** (after a warm-up phase).
- *AC total* = automaton construction + search. *KMP × k* = running KMP (prefix function + search) separately for each of the k patterns.
- In every run the number of matches found by both methods is compared, and they are always equal.

**Machine:** Intel Core i5-13450HX (2.40 GHz), 16 GB RAM, Windows 11 Pro.

### Results

| Patterns | Text length | Matches | Trie nodes | AC build (ms) | AC search (ms) | AC total (ms) | KMP × k (ms) | KMP / AC |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1,000 | 12 | 46 | 0.037 | 0.004 | 0.040 | 0.075 | 1.85x |
| 10 | 10,000 | 151 | 49 | 0.080 | 0.038 | 0.118 | 0.975 | 8.27x |
| 10 | 100,000 | 1,038 | 46 | 0.061 | 0.357 | 0.417 | 7.593 | 18.21x |
| 100 | 1,000 | 99 | 338 | 0.688 | 0.006 | 0.694 | 0.728 | 1.05x |
| 100 | 10,000 | 831 | 369 | 0.319 | 0.049 | 0.367 | 7.219 | 19.65x |
| 100 | 100,000 | 9,676 | 375 | 0.312 | 0.469 | 0.780 | 74.471 | 95.44x |
| 1,000 | 1,000 | 1,058 | 2,006 | 1.886 | 0.023 | 1.909 | 7.397 | 3.87x |
| 1,000 | 10,000 | 10,158 | 1,994 | 1.510 | 0.171 | 1.681 | 72.828 | 43.32x |
| 1,000 | 100,000 | 101,339 | 2,046 | 1.913 | 1.683 | 3.596 | 720.117 | 200.26x |

Raw data: [`results.csv`](results.csv).

### Discussion of the results

- **Aho–Corasick search time depends on the text length, not on the number of patterns.** For 1,000 patterns it grows from 0.171 ms (n = 10,000) to 1.683 ms (n = 100,000), about 10x for a 10x longer text, which matches O(n + z).
- **KMP time grows with both n and k.** For n = 100,000 it goes from 7.6 ms (10 patterns) to 74.5 ms (100) to 720 ms (1,000). It is linear in the number of patterns, as expected from O(k·n).
- **The advantage of Aho–Corasick grows with k and n.** On a 100,000-character text the speedup is 18x for 10 patterns, 95x for 100 patterns and 200x for 1,000 patterns.
- **Small inputs are the exception.** For 100 patterns and a 1,000-character text both methods are almost equal (1.05x), because building the automaton (0.688 ms) costs about as much as it saves.
- **Build time and memory follow the size of the trie.** The number of nodes grows with the number of patterns (about 46, 350 and 2,000 nodes) and stays below the total length of the patterns, because common prefixes are shared.
- **Very small timings are noisy** (JIT warm-up, timer resolution), so differences of a few hundredths of a millisecond should not be over-interpreted.

## Complexity analysis

Notation: `m` = total length of all patterns, `n` = text length, `k` = number of patterns, `σ` = alphabet size, `z` = number of reported occurrences.

| Part | Time | Space |
|---|---|---|
| Trie construction | O(m) | O(m·σ) with a full transition table |
| Failure + output links (BFS) | O(m·σ) with the full automaton (O(m) with sparse transitions) | O(m) |
| Search | O(n + z) | – |
| **Aho–Corasick total** | **O(m·σ + n + z)** | **O(m·σ)** |
| KMP for each pattern separately | O(m + k·n) | O(longest pattern) |

**Why the search is linear.** Every step moves one character forward through the automaton, and the text pointer never goes back. The failure links are already folded into the transitions, so there is no inner loop on mismatches. The `z` term is only the cost of reporting matches, and output links skip nodes that do not end a pattern.

## Role of the trie and failure links

- The **trie** merges common prefixes of all patterns, so the text is compared with all patterns at the same time instead of one by one.
- A **failure link** tells where to continue after a mismatch: it leads to the longest suffix of the text read so far that is still a prefix of some pattern. This is the same idea as the prefix function in KMP, but it works on a whole trie of patterns instead of one string, and no text character has to be read twice.
- **Output links** make sure that patterns ending inside the current match (patterns that are suffixes of the current string) are not missed.

## Why Aho–Corasick is suitable for multiple patterns

The text is scanned **once**, no matter how many patterns there are. Adding patterns only increases the preprocessing (building the trie and links), not the number of passes over the text. KMP per pattern needs `k` passes, so its time grows linearly with the number of patterns.

## Aho–Corasick vs. KMP for each pattern

| | Aho–Corasick | KMP for each pattern |
|---|---|---|
| Passes over the text | 1 | k |
| Time | O(m·σ + n + z) | O(m + k·n) |
| Memory | O(m·σ), trie with transitions | O(longest pattern) |
| Advantages | Search time almost independent of k; best for many patterns and long texts | Very simple, tiny memory, no shared structure; good for one pattern or patterns that change often |
| Limitations | Preprocessing and memory grow with total pattern length and alphabet; patterns must be known in advance; unnecessary for one or two patterns | k full passes over the text; becomes slow for hundreds of patterns |

**Conclusion.** The experiments confirm the theory: for 1,000 patterns on a 100,000-character text, Aho–Corasick is about 200 times faster than running KMP separately for each pattern. For very short texts and few patterns the difference is small, and KMP is a perfectly good choice.