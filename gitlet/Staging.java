package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class Staging implements Serializable {

    /** A treemap to record the filename and hashcode added to stage. */
    private TreeMap<String, String> addition;

    /** A treemap to record the filename and hashcode removed from stage. */
    private TreeMap<String, String> removal;

    public Staging() {
        this.addition = new TreeMap<>();
        this.removal = new TreeMap<>();
    }

    public TreeMap<String, String> getAddition() {
        return this.addition;
    }

    public TreeMap<String, String> getRemoval() {

        return this.removal;
    }

    public void add(String file, String hash) {
        this.addition.put(file, hash);
    }

    public void remove(String file) {
        this.removal.remove(file);
    }

    public void clear() {
        this.addition = new TreeMap<>();
        this.removal = new TreeMap<>();
    }

}
