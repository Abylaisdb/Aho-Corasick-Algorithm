import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;


public class Benchmark {
    static final int RUNS = 5;
    static final int[] PATTERN_COUNTS = {10, 100, 1000};
    static final int[] TEXT_LENGTHS = {1_000, 10_000, 100_000};
    static final int ALPHA = 4;
    static final int MIN_LEN = 4, MAX_LEN = 8;
    static final long SEED = 2530L;

    static volatile long sink;

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "results.csv");

        System.out.println("Java " + System.getProperty("java.version") + ", "
                + System.getProperty("os.name") + ", " + Runtime.getRuntime().availableProcessors() + " logical cores");
        warmUp();

        StringBuilder md = new StringBuilder();
        md.append("| Patterns | Text length | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP x k (ms) | KMP / AC |\n");
        md.append("|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out))) {
            csv.println("patterns,text_length,matches,ac_build_ms,ac_search_ms,ac_total_ms,kmp_ms,kmp_over_ac,trie_nodes");

            for (int k : PATTERN_COUNTS) {
                for (int n : TEXT_LENGTHS) {
                    Random rnd = new Random(SEED + k * 31L + n);
                    String text = randomString(rnd, n);
                    String[] pats = new String[k];
                    for (int i = 0; i < k; i++) {
                        pats[i] = randomString(rnd, MIN_LEN + rnd.nextInt(MAX_LEN - MIN_LEN + 1));
                    }

                    long buildSum = 0, searchSum = 0, kmpSum = 0;
                    long acMatches = 0, kmpMatches = 0;
                    int nodes = 0;
                    for (int r = 0; r < RUNS; r++) {
                        long t0 = System.nanoTime();
                        AhoCorasick ac = new AhoCorasick(pats);
                        long t1 = System.nanoTime();
                        acMatches = ac.count(text);
                        long t2 = System.nanoTime();
                        kmpMatches = KMP.countAll(text, pats);
                        long t3 = System.nanoTime();

                        buildSum += t1 - t0;
                        searchSum += t2 - t1;
                        kmpSum += t3 - t2;
                        nodes = ac.nodeCount();
                        sink += acMatches + kmpMatches;
                    }
                    if (acMatches != kmpMatches) {
                        throw new IllegalStateException("Mismatch AC=" + acMatches + " KMP=" + kmpMatches);
                    }

                    double build = buildSum / RUNS / 1e6;
                    double search = searchSum / RUNS / 1e6;
                    double total = build + search;
                    double kmp = kmpSum / RUNS / 1e6;
                    double ratio = kmp / total;

                    csv.printf(java.util.Locale.ROOT, "%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f,%d%n",
                            k, n, acMatches, build, search, total, kmp, ratio, nodes);
                    md.append(String.format(java.util.Locale.ROOT, "| %d | %d | %d | %.3f | %.3f | %.3f | %.3f | %.2fx |%n",
                            k, n, acMatches, build, search, total, kmp, ratio));
                    System.out.printf(java.util.Locale.ROOT,
                            "k=%-4d n=%-6d matches=%-8d AC build=%8.3f ms  AC search=%8.3f ms  KMP*k=%9.3f ms  (KMP/AC=%.2fx)%n",
                            k, n, acMatches, build, search, kmp, ratio);
                }
            }
        }
        System.out.println("\nMarkdown table:\n\n" + md);
        System.out.println("CSV written to " + out.toAbsolutePath());
    }

    private static void warmUp() {
        Random rnd = new Random(1);
        String text = randomString(rnd, 20_000);
        String[] pats = new String[50];
        for (int i = 0; i < pats.length; i++) pats[i] = randomString(rnd, 6);
        for (int i = 0; i < 30; i++) {
            sink += new AhoCorasick(pats).count(text) + KMP.countAll(text, pats);
        }
    }

    private static String randomString(Random r, int len) {
        char[] c = new char[len];
        for (int i = 0; i < len; i++) c[i] = (char) ('a' + r.nextInt(ALPHA));
        return new String(c);
    }
}
