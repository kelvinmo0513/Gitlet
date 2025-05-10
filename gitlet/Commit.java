package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Date;

public class Commit implements Serializable {
    /** Hash code of the commit object (UID). */
    private String hash;

    /** Hash code of the parent of the commit object. */
    private String parentHash;

    /** Timestamp of the commit object being created. */
    private String timestamp;

    /** Commit message being passed in. */
    private String message;

    /** A treemap to save both the filenames and its blob hashcode. */
    private TreeMap<String, String> mapping;

    /** An arraylst to record all the parents of one commit. */
    private ArrayList<String> allParents;

    public Commit(String note, String parent, TreeMap<String, String> blob) {
        SimpleDateFormat ts = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        this.timestamp = ts.format(new Date());
        this.message = note;
        this.mapping = blob;
        this.parentHash = parent;
        this.hash = getHash();
        this.allParents = new ArrayList<String>();
        this.allParents.add(this.parentHash);
    }

    public String getHash() {
        return Utils.sha1(Utils.serialize(this));
    }

    public String getParentHash() {
        return this.parentHash;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getMessage() {
        return this.message;
    }

    public TreeMap<String, String> getMapping() {
        return this.mapping;
    }

    public void addParent(String parentHash) {
        this.allParents.add(parentHash);
    }

    public ArrayList<String> getAllParents() {
        return this.allParents;
    }

}
