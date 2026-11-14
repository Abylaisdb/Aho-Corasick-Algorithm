import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AhoCorasick {

    public static final int ALPHABET = 256;

    public static final class Match {
        public final int start;
        public final int patternIndex;

        public Match(int start, int patternIndex) {
            this.start = start;
            this.patternIndex = patternIndex;
        }

        @Override
        public String toString() {
            return "(start=" + start + ", pattern=" + patternIndex + ")";
        }
    }

    private final String[] patterns;
    private int[][] next;
    private int[] fail;
    private int[] outLink;
    private List<List<Integer>> endsHere;
    private int size;

    public AhoCorasick(String[] patterns) {
        this.patterns = patterns.clone();
        buildTrie();
        buildFailureLinks();
    }

    private void buildTrie() {
        int maxNodes = 1;
        for (String p : patterns) {
            if (p == null || p.isEmpty()) {
                throw new IllegalArgumentException("Patterns must be non-empty");
            }
            maxNodes += p.length();
        }
        next = new int[maxNodes][];
        fail = new int[maxNodes];
        outLink = new int[maxNodes];
        endsHere = new ArrayList<>(maxNodes);

        newNode();

        for (int id = 0; id < patterns.length; id++) {
            int node = 0;
            for (int i = 0; i < patterns[id].length(); i++) {
                int c = patterns[id].charAt(i);
                checkChar(c);
                if (next[node][c] == -1) {
                    next[node][c] = newNode();
                }
                node = next[node][c];
            }
            if (endsHere.get(node) == null) {
                endsHere.set(node, new ArrayList<>(1));
            }
            endsHere.get(node).add(id);
        }
    }

    private int newNode() {
        int id = size++;
        next[id] = new int[ALPHABET];
        Arrays.fill(next[id], -1);
        outLink[id] = -1;
        endsHere.add(null);
        return id;
    }

    private static void checkChar(int c) {
        if (c >= ALPHABET) {
            throw new IllegalArgumentException("Unsupported character (code " + c + "), only 0-255 allowed");
        }
    }

    private void buildFailureLinks() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int c = 0; c < ALPHABET; c++) {
            int v = next[0][c];
            if (v == -1) {
                next[0][c] = 0;
            } else {
                fail[v] = 0;
                outLink[v] = endsHere.get(0) != null ? 0 : -1;
                queue.add(v);
            }
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int c = 0; c < ALPHABET; c++) {
                int v = next[u][c];
                if (v == -1) {
                    next[u][c] = next[fail[u]][c];
                } else {
                    int f = next[fail[u]][c];
                    fail[v] = f;
                    outLink[v] = (endsHere.get(f) != null) ? f : outLink[f];
                    queue.add(v);
                }
            }
        }
    }

    public List<Match> search(String text) {
        List<Match> result = new ArrayList<>();
        int node = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            node = (c < ALPHABET) ? next[node][c] : 0;

            int out = (endsHere.get(node) != null) ? node : outLink[node];
            while (out != -1) {
                for (int id : endsHere.get(out)) {
                    result.add(new Match(i - patterns[id].length() + 1, id));
                }
                out = outLink[out];
            }
        }
        return result;
    }

    public long count(String text) {
        long total = 0;
        int node = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            node = (c < ALPHABET) ? next[node][c] : 0;
            int out = (endsHere.get(node) != null) ? node : outLink[node];
            while (out != -1) {
                total += endsHere.get(out).size();
                out = outLink[out];
            }
        }
        return total;
    }

    public int nodeCount() {
        return size;
    }

    public int[] failureLinks() {
        return Arrays.copyOf(fail, size);
    }
}
