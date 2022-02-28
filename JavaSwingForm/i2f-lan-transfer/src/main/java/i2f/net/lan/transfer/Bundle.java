package i2f.net.lan.transfer;

import java.util.HashMap;

/**
 * @author ltb
 * @date 2022/2/28 15:45
 * @desc
 */
public class Bundle extends HashMap<String,Object> {
    public void putStringArray(String key, String[] arr) {
        this.put(key,arr);
    }

    public String[] getStringArray(String key) {
        return (String[])this.get(key);
    }
}
