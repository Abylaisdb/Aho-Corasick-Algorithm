import java.util.ArrayList;
import java.util.List;

public class KMP {

    public static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        int k = 0;
        for (int i = 1; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) {
                k = pi[k - 1];
            }
            if (p.charAt(i) == p.charAt(k)) {
                k++;
            }
            pi[i] = k;
        }
        return pi;
    }

    public static List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int[] pi = prefixFunction(pattern);
        int k = 0;
        for (int i = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (text.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            if (k == pattern.length()) {
                result.add(i - k + 1);
                k = pi[k - 1];
            }
        }
        return result;
    }

    public static long count(String text, String pattern) {
        long total = 0;
        int[] pi = prefixFunction(pattern);
        int k = 0;
        for (int i = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (text.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            if (k == pattern.length()) {
                total++;
                k = pi[k - 1];
            }
        }
        return total;
    }

    public static long countAll(String text, String[] patterns) {
        long total = 0;
        for (String p : patterns) {
            total += count(text, p);
        }
        return total;
    }
}
