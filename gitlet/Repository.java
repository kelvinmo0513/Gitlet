package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.List;

public class Repository implements Serializable {

    /** File object of the current user directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** File object of the current blobs directory. */
    public static final File Blobs = Utils.join(CWD, ".gitlet/blobs/");

    /** File object of the current Branches directory. */
    public static final File Branches = Utils.join(CWD, ".gitlet/Branches/");

    /** File object of the current Commits directory. */
    public static final File Commits = Utils.join(CWD, ".gitlet/Commits/");

    /** File object of the current staging directory. */
    public static final File Staging = Utils.join(CWD, ".gitlet/staging/");

    /** File object of the current gitlet directory. */
    public static final File Gitlet = Utils.join(CWD, ".gitlet/");

    /** Stage object to store the staging area. */
    private Staging stage;

    /** Pointer that points to the current branch. */
    private String head;

    /** Variable to store tehe current commit object. */
    private Commit commit;

    /** A treemap to map the Branches with their current commit. */
    private TreeMap<String, Commit> branches;

    public Repository() {
        File currCommit = Utils.join(Branches, "head");
        if (currCommit.exists()) {
            this.head = Utils.readContentsAsString(currCommit);
            String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
            this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
            File stage_dir = Utils.join(Staging, "stage");
            this.stage = Utils.readObject(stage_dir, Staging.class);
        } else {
            this.head = "master";
            this.commit = null;
        }
        this.branches = new TreeMap<String, Commit>();
    }

    public void init() {
        if (Gitlet.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }

        Gitlet.mkdir();
        Blobs.mkdir();
        Commits.mkdir();
        Staging.mkdir();
        Branches.mkdir();

        String msg = "initial commit";
        this.commit = new Commit(msg, null, new TreeMap<String, String>());
        branches.put("master", this.commit);
        Utils.writeObject(Utils.join(Commits, this.commit.getHash()), this.commit);
        Utils.writeObject(Utils.join(Staging, "stage"), new Staging());
        Utils.writeContents(Utils.join(Branches, "master"), this.commit.getHash());
        Utils.writeContents(Utils.join(Branches, "head"), "master");
    }

    public void add(String file) {
        File newFile = new File(file);
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String contents = Utils.readContentsAsString(Utils.join(CWD, file));
        String hash = Utils.sha1(contents);
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        if (this.commit.getMapping().containsKey(file)) {
            if (!hash.equals(this.commit.getMapping().get(file))) {
                Utils.writeContents(Utils.join(Blobs, hash), Utils.readContentsAsString(newFile));
                this.stage.add(file, hash);
            }
        }
        if (this.commit == null) {
            Utils.writeContents(Utils.join(Staging, file), Utils.readContents(newFile));
            return;
        }
        if (this.commit.getMapping().containsKey(file)) {
            if (this.commit.getMapping().get(file).equals(hash)) {
                if (this.stage.getRemoval().containsKey(file)) {
                    this.stage.remove(file);
                    Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
                }
                return;
            }
        }
        this.stage.add(file, Utils.sha1(Utils.readContents(newFile)));
        Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
        Utils.writeContents(Utils.join(Blobs, hash), Utils.readContentsAsString(newFile));
    }

    public void commit(String note) {
        this.stage = Utils.readObject(Utils.join(Staging, "stage"), Staging.class);
        TreeMap<String, String> filesToAdd = this.stage.getAddition();
        TreeMap<String, String> filesToRemove = this.stage.getRemoval();
        if (filesToAdd.size() == 0 && filesToRemove.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (note.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        TreeMap<String, String> newBlobs = (TreeMap<String, String>) this.commit.getMapping().clone();
        for (String add: filesToAdd.keySet()) {
            String contents = Utils.readContentsAsString(new File(add));
            String addHash = Utils.sha1(contents);
            newBlobs.put(add, addHash);
        }
        for (String remove: filesToRemove.keySet()) {
            newBlobs.remove(remove);
        }
        Commit newCommit = new Commit(note, this.commit.getHash(), newBlobs);
        this.commit = newCommit;
        Utils.writeContents(Utils.join(Branches, this.head), newCommit.getHash());
        Utils.writeObject(Utils.join(Commits, newCommit.getHash()), newCommit);
        branches.replace(this.head, commit, newCommit);
        this.stage.clear();
        Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
    }

    public void remove(String filename) {
        File file = new File(filename);
        this.stage = Utils.readObject(Utils.join(Staging, "stage"), Staging.class);
        TreeMap<String, String> filesToAdd = this.stage.getAddition();
        TreeMap<String, String> filesToRemove = this.stage.getRemoval();
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);

        if (!filesToAdd.containsKey(filename) && !this.commit.getMapping().containsKey(filename)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (filesToAdd.containsKey(filename)) {
            filesToAdd.remove(filename);
            Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
        }
        if (this.commit.getMapping().containsKey(filename)) {
            if (file.exists()) {
                Utils.restrictedDelete(filename);
            }
            filesToRemove.put(filename, Utils.sha1(filename));
            Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
        }
    }

    public void log() {
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        while (commit.getParentHash() != null) {
            System.out.println("===");
            System.out.println("commit " + commit.getHash());
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage());
            System.out.println();
            String parent = commit.getParentHash();
            commit = Utils.readObject(Utils.join(Commits, parent), Commit.class);
        }
        System.out.println("===");
        System.out.println("commit " + commit.getHash());
        System.out.println("Date: " + commit.getTimestamp());
        System.out.println(commit.getMessage());
        return;
    }

    public void global_log() {
        List<String> commitList = Utils.plainFilenamesIn(Commits);
        for (int i = 0; i < commitList.size(); i++) {
            if (i == commitList.size() - 1) {
                commit = Utils.readObject(Utils.join(Commits, commitList.get(i)), Commit.class);
                System.out.println("===");
                System.out.println("commit " + commit.getHash());
                System.out.println("Date: " + commit.getTimestamp());
                System.out.println(commit.getMessage());
            } else {
                commit = Utils.readObject(Utils.join(Commits, commitList.get(i)), Commit.class);
                System.out.println("===");
                System.out.println("commit " + commit.getHash());
                System.out.println("Date: " + commit.getTimestamp());
                System.out.println(commit.getMessage());
                System.out.println();
            }
        }
    }

    public void find(String msg) {
        Boolean find = false;
        List<String> commitList = Utils.plainFilenamesIn(Commits);
        for (int i = 0; i < commitList.size(); i++) {
            commit = Utils.readObject(Utils.join(Commits, commitList.get(i)), Commit.class);
            if (commit.getMessage().equals(msg)) {
                find = true;
                System.out.println(commit.getHash());
            }
        }
        if (!find) {
            System.out.println("Found no commit with that message.");
            return;
        }
    }

    public void status() {
        this.stage = Utils.readObject(Utils.join(Staging, "stage"), Staging.class);
        TreeMap<String, String> filesToAdd = this.stage.getAddition();
        TreeMap<String, String> filesToRemove = this.stage.getRemoval();
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);

        System.out.println("=== Branches ===");
        List<String> branchList = Utils.plainFilenamesIn(Branches);
        for (int i = 0; i < branchList.size(); i++) {
            if (branchList.get(i).equals(this.head)) {
                System.out.println("*" + this.head);
            } else {
                if (branchList.get(i).equals("head")) {
                    continue;
                }
                System.out.println(branchList.get(i));
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (String key: filesToAdd.keySet()) {
            System.out.println(key);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String key: filesToRemove.keySet()) {
            System.out.println(key);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public void checkout(String[] args) {
        if (args.length == 2) {
            if (args[1].equals(this.head)) {
                System.out.println("No need to checkout the current branch.");
                return;
            }
            if (!Utils.join(Branches, args[1]).exists()) {
                System.out.println("No such branch exists.");
                return;
            }
            String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
            this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
            String newCommitHash = Utils.readContentsAsString(Utils.join(Branches, args[1]));
            Commit newCommit = Utils.readObject(Utils.join(Commits, newCommitHash), Commit.class);
            for (String file: Utils.plainFilenamesIn(CWD)) {
                if (!this.commit.getMapping().containsKey(file) && newCommit.getMapping().containsKey(file)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
            }
            for (String file: newCommit.getMapping().keySet()) {
                if (newCommit.getMapping().keySet().contains(file)) {
                    String newHash = newCommit.getMapping().get(file);
                    File newFile = Utils.join(Blobs, newHash);
                    Utils.writeContents(Utils.join(CWD, file), Utils.readContentsAsString(newFile));
                }
            }
            for (String file: Utils.plainFilenamesIn(CWD)) {
                if (this.commit.getMapping().containsKey(file) && !newCommit.getMapping().containsKey(file)) {
                    Utils.restrictedDelete(file);
                }
            }
            this.stage.clear();
            Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
            this.head = args[1];
            this.commit = newCommit;
            Utils.writeContents(Utils.join(Branches, "head"), args[1]);
        } else if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
            commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
            String filename = args[2];
            String hash = commit.getMapping().get(filename);
            if (hash == null) {
                System.out.println("File does not exist in that commit.");
                return;
            } else {
                String overwrite = Utils.readContentsAsString(Utils.join(Blobs, hash));
                Utils.writeContents(new File(filename), overwrite);
            }
        } else if (args.length == 4) {
            List<String> commitList = Utils.plainFilenamesIn(Commits);
            boolean found = false;
            String fullCommitID = null;
            String commitID = args[1];
            String filename = args[3];
            for (String file: commitList) {
                if ((file.substring(0, commitID.length())).equals(commitID)) {
                    found = true;
                    fullCommitID = file;
                    break;
                }
            }
            if (!found) {
                System.out.println("No commit with that id exists.");
                return;
            }
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            if (!commit.getMapping().containsKey(filename)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
            commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
            String[] listOfCommits = Commits.list();
            for (String hash: listOfCommits) {
                if (commitID.equals(hash.substring(0, commitID.length()))) {
                    commit = Utils.readObject(Utils.join(Commits, fullCommitID), Commit.class);
                }
            }
            String newHash = commit.getMapping().get(filename);
            String newBlob = Utils.readContentsAsString(Utils.join(Blobs, newHash));
            Utils.writeContents(new File(filename), newBlob);
        } else {
            System.out.println("Incorrect number of arguments.");
            return;
        }
    }

    public void branch(String newBranch) {
        List<String> branchList = Utils.plainFilenamesIn(Branches);
        if (branchList.contains(newBranch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        branches.put(newBranch, this.commit);
        Utils.writeContents(Utils.join(Branches, newBranch), commitHash);
    }

    public void rm_branch(String branch) {
        List<String> branchList = Utils.plainFilenamesIn(Branches);
        if (!branchList.contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branch.equals(this.head)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        Utils.join(Branches, branch).delete();
        branches.remove(branch);
    }

    public void reset(String commitID) {
        List<String> commitList = Utils.plainFilenamesIn(Commits);
        if (!commitList.contains(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        Commit newCommit = Utils.readObject(Utils.join(Commits, commitID), Commit.class);
        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (!this.commit.getMapping().containsKey(file) && newCommit.getMapping().containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (this.commit.getMapping().containsKey(file) && !newCommit.getMapping().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }
        this.stage.clear();
        Utils.writeObject(Utils.join(Staging, "stage"), this.stage);
        this.commit = newCommit;
        Utils.writeContents(Utils.join(Branches, this.head), commitID);
    }

    public void merge(String branch) {
        this.stage = Utils.readObject(Utils.join(Staging, "stage"), Staging.class);
        TreeMap<String, String> filesToAdd = this.stage.getAddition();
        TreeMap<String, String> filesToRemove = this.stage.getRemoval();
        if (!filesToAdd.isEmpty() || !filesToRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        List<String> branchList = Utils.plainFilenamesIn(Branches);
        if (!branchList.contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (this.head.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String commitHash = Utils.readContentsAsString(Utils.join(Branches, this.head));
        this.commit = Utils.readObject(Utils.join(Commits, commitHash), Commit.class);
        String newCommitHash = Utils.readContentsAsString(Utils.join(Branches, branch));
        Commit newCommit = Utils.readObject(Utils.join(Commits, newCommitHash), Commit.class);
        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (!this.commit.getMapping().containsKey(file) && newCommit.getMapping().containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }

        Commit currCommitTemp = this.commit;
        Commit newCommitTemp = newCommit;
        Commit splitCommit = null;
        while (currCommitTemp.getParentHash() != null) {
            while (newCommitTemp.getParentHash() != null) {
                if (currCommitTemp.getHash().equals(newCommitTemp.getHash())) {
                    splitCommit = currCommitTemp;
                    break;
                }
                newCommitTemp = Utils.readObject(Utils.join(Commits, newCommitTemp.getParentHash()), Commit.class);
            }
            currCommitTemp = Utils.readObject(Utils.join(Commits, currCommitTemp.getParentHash()), Commit.class);
            newCommitTemp = newCommit;
        }

        if (splitCommit.getHash().equals(this.commit.getHash())) {
            Utils.writeContents(Utils.join(Branches, this.head), newCommitHash);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (splitCommit.getHash().equals(newCommit.getHash())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (this.commit.getMapping().containsKey(file) && !newCommit.getMapping().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }
        for (String file: newCommit.getMapping().keySet()) {
            if (newCommit.getMapping().keySet().contains(file)) {
                String newHash = newCommit.getMapping().get(file);
                File newFile = Utils.join(Blobs, newHash);
                Utils.writeContents(Utils.join(CWD, file), Utils.readContentsAsString(newFile));
            }
        }
        System.out.println("Current branch fast-forwarded.");
        return;
    }

}
