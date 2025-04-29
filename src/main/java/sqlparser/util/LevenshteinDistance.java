package sqlparser.util;

public class LevenshteinDistance {
        /**
         * Computes the Levenshtein distance between two strings.
         *
         * @param s1 First string
         * @param s2 Second string
         * @return The Levenshtein distance between the two strings
         */
        public static int compute(String s1, String s2) {
                int lenS1 = s1.length();
                int lenS2 = s2.length();

                // Create a distance matrix
                int[][] distance = new int[lenS1 + 1][lenS2 + 1];

                // Initialize the matrix
                for (int i = 0; i <= lenS1; i++) {
                distance[i][0] = i;
                }
                for (int j = 0; j <= lenS2; j++) {
                distance[0][j] = j;
                }

                // Compute the distances
                for (int i = 1; i <= lenS1; i++) {
                for (int j = 1; j <= lenS2; j++) {
                        int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                        distance[i][j] = Math.min(Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1), distance[i - 1][j - 1] + cost);
                }
                }

                return distance[lenS1][lenS2];
        }
}
