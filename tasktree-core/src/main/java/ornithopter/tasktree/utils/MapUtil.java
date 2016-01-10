package ornithopter.tasktree.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ornithopter on 2016/1/10.
 */
public final class MapUtil {
    public static Map<String, String> from(String... mappings) {
        if (mappings.length % 2 != 0) {
            throw new IllegalArgumentException("length of mappings must be even.");
        }

        HashMap<String, String> result = new HashMap<>(mappings.length / 2);
        for (int i = 0; i < mappings.length; i++) {
            result.put(mappings[i++], mappings[i]);
        }
        return result;
    }
}
