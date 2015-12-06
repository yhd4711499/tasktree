package ornithopter.tasktree.compiler.utils;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ornithopter on 2015/11/22.
 */
public class StringUtils {
    public static String join(Collection var0, String var1) {
        StringBuilder var2 = new StringBuilder();

        for (Iterator var3 = var0.iterator(); var3.hasNext(); var2.append((String) var3.next())) {
            if (var2.length() != 0) {
                var2.append(var1);
            }
        }

        return var2.toString();
    }
}
