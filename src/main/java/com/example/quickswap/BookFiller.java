package com.example.quickswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * When a book is signed with a trigger title ("sudoku", "origami"), replaces its pages
 * with 100 generated pages before the packet is sent to the server.
 */
public final class BookFiller {
    private BookFiller() {}

    public static final int PAGES = 100;

    /** Called from the packet mixin for every outgoing packet. Returns the packet to actually send. */
    public static Packet<?> maybeRewrite(Packet<?> packet) {
        try {
            if (!QuickSwapConfig.get().bookFill) return packet;
            if (packet instanceof BookUpdateC2SPacket book && book.title().isPresent()) {
                String title = book.title().get().trim().toLowerCase(Locale.ROOT);
                List<String> pages = generate(title);
                if (pages != null) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("QuickSwap: filled the book with "
                                + PAGES + " " + title + " pages"), true);
                    }
                    return new BookUpdateC2SPacket(book.slot(), pages, book.title());
                }
            }
        } catch (Throwable t) {
            QuickSwapClient.LOGGER.error("Book fill failed, sending the book unchanged", t);
        }
        return packet;
    }

    /** Returns 100 pages for a known title, or null if the title isn't a trigger. */
    public static List<String> generate(String lowerTitle) {
        return switch (lowerTitle) {
            case "sudoku" -> sudokuPages();
            case "origami" -> origamiPages();
            default -> null;
        };
    }

    // ------------------------------------------------------------------ sudoku

    private static List<String> sudokuPages() {
        Random rnd = new Random();
        List<String> pages = new ArrayList<>(PAGES);
        for (int i = 1; i <= PAGES; i++) {
            pages.add(sudokuPage(makePuzzle(rnd), i));
        }
        return pages;
    }

    /**
     * Minecraft's font is proportional, but digits, '_' and spaces have fixed widths, so every
     * row lines up as long as each cell is exactly one digit or one underscore.
     */
    private static String sudokuPage(int[] c, int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sudoku ").append(n).append('/').append(PAGES).append("\n\n");
        for (int r = 0; r < 9; r++) {
            if (r == 3 || r == 6) sb.append("----------------\n");
            for (int col = 0; col < 9; col++) {
                int v = c[r * 9 + col];
                sb.append(v == 0 ? '_' : (char) ('0' + v));
                if (col == 2 || col == 5) sb.append(" | ");
                else if (col < 8) sb.append(' ');
            }
            if (r < 8) sb.append('\n');
        }
        return sb.toString();
    }

    private static int[] makePuzzle(Random rnd) {
        int[] solved = new int[81];
        fillGrid(solved, rnd);

        int[] puzzle = solved.clone();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 81; i++) order.add(i);
        java.util.Collections.shuffle(order, rnd);

        int targetBlanks = 44 + rnd.nextInt(8);
        int blanks = 0;
        long deadline = System.nanoTime() + 30_000_000L; // 30 ms budget per puzzle
        for (int pos : order) {
            if (blanks >= targetBlanks || System.nanoTime() > deadline) break;
            int saved = puzzle[pos];
            puzzle[pos] = 0;
            if (countSolutions(puzzle.clone(), 2) != 1) {
                puzzle[pos] = saved; // removing it made the puzzle ambiguous
            } else {
                blanks++;
            }
        }
        return puzzle;
    }

    private static int candidates(int[] c, int idx) {
        int row = idx / 9, col = idx % 9, used = 0;
        for (int i = 0; i < 9; i++) {
            used |= 1 << c[row * 9 + i];
            used |= 1 << c[i * 9 + col];
        }
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int k = 0; k < 3; k++) used |= 1 << c[(br + r) * 9 + bc + k];
        }
        return ~used & 0x3FE; // bits 1..9
    }

    private static boolean fillGrid(int[] c, Random rnd) {
        int idx = -1;
        for (int i = 0; i < 81; i++) {
            if (c[i] == 0) { idx = i; break; }
        }
        if (idx < 0) return true;

        List<Integer> digits = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        java.util.Collections.shuffle(digits, rnd);
        int mask = candidates(c, idx);
        for (int d : digits) {
            if ((mask & (1 << d)) == 0) continue;
            c[idx] = d;
            if (fillGrid(c, rnd)) return true;
            c[idx] = 0;
        }
        return false;
    }

    private static int countSolutions(int[] c, int limit) {
        int best = -1, bestMask = 0, bestCount = 10;
        for (int i = 0; i < 81; i++) {
            if (c[i] != 0) continue;
            int mask = candidates(c, i);
            int n = Integer.bitCount(mask);
            if (n == 0) return 0;
            if (n < bestCount) {
                best = i;
                bestMask = mask;
                bestCount = n;
                if (n == 1) break;
            }
        }
        if (best < 0) return 1;

        int total = 0;
        for (int d = 1; d <= 9 && total < limit; d++) {
            if ((bestMask & (1 << d)) == 0) continue;
            c[best] = d;
            total += countSolutions(c, limit - total);
        }
        c[best] = 0;
        return total;
    }

    // ----------------------------------------------------------------- origami

    private static final String[] BASES = {
            "Square base", "Bird base", "Waterbomb", "Fish base", "Kite base", "Frog base", "Preliminary"
    };
    private static final String[] STEPS = {
            "Valley fold", "Mountain fold", "Squash fold", "Reverse fold", "Crimp fold", "Petal fold",
            "Rabbit ear", "Sink the tip", "Pleat the edge", "Tuck the flap", "Fold to center",
            "Unfold, flip", "Pull the wings", "Curl the tip", "Crease, unfold", "Flatten gently",
            "Fold the corner", "Turn it over", "Rotate 90 deg."
    };
    private static final String[] ADJ = {"Tiny", "Lazy", "Bold", "Calm", "Swift", "Proud", "Sly", "Neat", "Quiet"};
    private static final String[] NOUN = {
            "Crane", "Fox", "Frog", "Boat", "Tulip", "Heron", "Koi", "Lotus", "Whale", "Swan", "Star", "Box"
    };

    private static List<String> origamiPages() {
        Random rnd = new Random();
        List<String> pages = new ArrayList<>(PAGES);
        for (int i = 1; i <= PAGES; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("Origami ").append(i).append('/').append(PAGES).append("\n\n");
            sb.append("Base: ").append(BASES[rnd.nextInt(BASES.length)]).append('\n');

            // Pick 7 different steps in random order.
            List<String> pool = new ArrayList<>(List.of(STEPS));
            java.util.Collections.shuffle(pool, rnd);
            for (int s = 0; s < 7; s++) {
                sb.append(s + 1).append(". ").append(pool.get(s)).append('\n');
            }
            sb.append('\n').append("= ")
                    .append(ADJ[rnd.nextInt(ADJ.length)]).append(' ')
                    .append(NOUN[rnd.nextInt(NOUN.length)]);
            pages.add(sb.toString());
        }
        return pages;
    }
}
