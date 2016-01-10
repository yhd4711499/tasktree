package ornithopter.tasktree;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ornithopter on 2016/1/10.
 */
class TaskConnector {
    /**
     * <code>mapping</code> is used for converting result from producer to appropriate format for consumer
     * <p>
     * For example:
     * <li>Task [A] produces {"info": infoA}</li>
     * <li>Task [B] produces {"info": infoB}</li>
     * <li>Task [C] consumes these two info (as fields "infoA" and "infoB").</li>
     * <p>
     *
     * <i>note: {"this is the field name": this_is_the_object}</i>
     *
     * <p>
     * To achieve this, you can use {"infoA":"info"} for Task [A] and {"infoB":"info"} for Task [B] as shown below.
     * <p>
     * <code><pre>
     * |    result from [A] and [B]     |       mapping         |       converted       |
     * ----------------------------------------------------------------------------------
     * |    Task [A]: {"info": infoA} --|-> {"infoA":"info"}    |   {"infoA": infoA     |
     * |    Task [B]: {"info": infoB} --|-> {"infoB":"info"}    |    "infoB": infoB}    |
     * </pre></code>
     *
     * @param mapping mapping (from consumer to producer)
     *
     */
    public TaskConnector(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    private final Map<String, String> mapping;

    /**
     * convert result from producer to appropriate format for consumer
     *
     * @param rawResult result from producer
     * @return appropriate format for consumer
     */
    Map<String, Object> mapResultForConsumer(Map<String, Object> rawResult) {
        HashMap<String, Object> mapped = new HashMap<>(mapping.size());
        for (String mappedKey : mapping.keySet()) {
            mapped.put(mappedKey, rawResult.get(mapping.get(mappedKey)));
        }
        return mapped;
    }
}
