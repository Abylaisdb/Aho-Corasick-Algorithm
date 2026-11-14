import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Tests {
    private static int passed = 0, failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS  " + name); }
        else    { failed++; System.out.println("FAIL  " + name); }
    }

    private static Set<String> toSet(List<AhoCorasick.Match> ms) {
        Set<String> s = new HashSet<>();
        for (AhoCorasick.Match m : ms) s.add(m.start + ":" + m.patternIndex);
        return s;
    }

    private static Set<String> brute(String text, String[] pats) {
        Set<String> s = new HashSet<>();
        for (int id = 0; id < pats.length; id++)
            for (int i = 0; i + pats[id].length() <= text.length(); i++)
                if (text.startsWith(pats[id], i)) s.add(i + ":" + id);
        return s;
    }

    public static void main(String[] args) {
        String[] p1 = {"he", "she", "his", "hers"};
        String t1 = "ushers";
        check("classic: ushers", toSet(new AhoCorasick(p1).search(t1)).equals(brute(t1, p1))
                && new AhoCorasick(p1).search(t1).size() == 3);

        String[] p2 = {"a", "aa", "aaa"};
        String t2 = "aaaa";
        check("nested/overlapping: aaaa", new AhoCorasick(p2).search(t2).size() == 4 + 3 + 2
                && toSet(new AhoCorasick(p2).search(t2)).equals(brute(t2, p2)));

        String[] p3 = {"abcd", "bcd", "cd", "d"};
        String t3 = "abcd";
        check("suffix chain via output links", new AhoCorasick(p3).search(t3).size() == 4);

        check("no matches", new AhoCorasick(new String[]{"xyz"}).search("abcabc").isEmpty());

        String[] p4 = {"ab", "ab"};
        check("duplicate patterns", new AhoCorasick(p4).search("abab").size() == 4);

        check("pattern == text", new AhoCorasick(new String[]{"hello"}).search("hello").size() == 1);
        check("pattern longer than text", new AhoCorasick(new String[]{"hello!"}).search("hello").isEmpty());

        check("empty text", new AhoCorasick(new String[]{"a"}).search("").isEmpty());

        boolean thrown = false;
        try { new AhoCorasick(new String[]{""}); } catch (IllegalArgumentException e) { thrown = true; }
        check("empty pattern rejected", thrown);

        AhoCorasick ac = new AhoCorasick(p1);
        check("node count (he,she,his,hers) = 10", ac.nodeCount() == 10);
        check("failure link of root is 0", ac.failureLinks()[0] == 0);

        check("KMP prefix function", Arrays.equals(KMP.prefixFunction("abacaba"), new int[]{0, 0, 1, 0, 1, 2, 3}));
        check("KMP overlapping", KMP.search("aaaa", "aa").equals(Arrays.asList(0, 1, 2)));

        Random rnd = new Random(42);
        boolean allOk = true;
        for (int iter = 0; iter < 300; iter++) {
            int alpha = 2 + rnd.nextInt(3);
            int nPat = 1 + rnd.nextInt(15);
            String[] pats = new String[nPat];
            for (int i = 0; i < nPat; i++) pats[i] = randomString(rnd, 1 + rnd.nextInt(6), alpha);
            String text = randomString(rnd, rnd.nextInt(200), alpha);
            AhoCorasick a = new AhoCorasick(pats);
            List<AhoCorasick.Match> got = a.search(text);
            Set<String> expected = brute(text, pats);
            if (!toSet(got).equals(expected) || got.size() != expected.size()
                    || a.count(text) != got.size() || KMP.countAll(text, pats) != got.size()) {
                allOk = false;
                break;
            }
        }
        check("300 random cases: Aho-Corasick == brute force == KMP", allOk);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static String randomString(Random r, int len, int alpha) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append((char) ('a' + r.nextInt(alpha)));
        return sb.toString();
    }
}
