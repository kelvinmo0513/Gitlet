package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kelvin Mo
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        File gitlet = new File(".gitlet");
        Repository repo = new Repository();
        switch (args[0]) {
        case "init":
            repo.init();
            break;
        case "add":
            if (!argCheck()) {
                break;
            }
            repo.add(args[1]);
            break;
        case "commit":
            if (!argCheck()) {
                break;
            }
            repo.commit(args[1]);
            break;
        case "rm":
            if (!argCheck()) {
                break;
            }
            repo.remove(args[1]);
            break;
        case "log":
            if (!argCheck()) {
                break;
            }
            repo.log();
            break;
        case "global-log":
            if (!argCheck()) {
                break;
            }
            repo.global_log();
            break;
        case "find":
            if (!argCheck()) {
                break;
            }
            repo.find(args[1]);
            break;
        case "status":
            if (!argCheck()) {
                break;
            }
            repo.status();
            break;
        case "checkout":
            if (!argCheck()) {
                break;
            }
            repo.checkout(args);
            break;
        case "branch":
            if (!argCheck()) {
                break;
            }
            repo.branch(args[1]);
            break;
        case "rm-branch":
            if (!argCheck()) {
                break;
            }
            repo.rm_branch(args[1]);
            break;
        case "reset":
            if (!argCheck()) {
                break;
            }
            repo.reset(args[1]);
            break;
        case "merge":
            if (!argCheck()) {
                break;
            }
            repo.merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            return;
        }
        return;
    }


    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(-1);
    }

    public static boolean argCheck() {
        if(!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return false;
        }
        return true;
    }

}
