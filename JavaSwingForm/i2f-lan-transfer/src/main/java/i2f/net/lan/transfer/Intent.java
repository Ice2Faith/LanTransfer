package i2f.net.lan.transfer;

import java.util.HashMap;

/**
 * @author ltb
 * @date 2022/2/28 15:24
 * @desc
 */
public class Intent extends HashMap<String,Object> {
    public void putExtra(String key, Object val) {
        this.put(key,val);
    }

    public int getIntExtra(String key, int defVal) {
        Integer val=(Integer) this.get(key);
        if (val == null) {
            val=defVal;
        }
        return val;
    }

    public String getStringExtra(String key) {
        return (String)this.get(key);
    }

    public boolean getBooleanExtra(String key, boolean defVal) {
        Boolean val=(Boolean)this.get(key);
        if(val==null){
            val=defVal;
        }
        return val;
    }

    public Bundle getBundleExtra(String key) {
        return (Bundle)this.get(key);
    }
}
